package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.*;
import hudson.EnvVars;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CloudFormationTest {

	private static final String TEST_STACK = "testStack";

	private CloudFormation cf; // SUT

	private String recipeBody = "recipe body";
	private Map<String, String> parameters = new HashMap<String, String>();
	private String awsAccessKey = "accessKey";
	private String awsSecretKey = "secretKey";
	private String awsRegion = "region";

	@Mock
	private AmazonCloudFormation awsClient;

	@Before
	public void setup() throws Exception {

		cf = new CloudFormation(System.out, TEST_STACK, recipeBody, parameters,
				-12345, awsAccessKey, awsSecretKey, awsRegion, true, new EnvVars()) {
			@Override
			protected AmazonCloudFormation getAWSClient() {
				return awsClient;
			}
		};

		when(awsClient.createStack(any(CreateStackRequest.class))).thenReturn(
				createResultWithId(TEST_STACK));
		when(
				awsClient
						.describeStackEvents(any(DescribeStackEventsRequest.class)))
				.thenReturn(new DescribeStackEventsResult());

	}

	@Test
	public void cloudFormationCreate_Wait_for_Stack_To_Be_Created()
			throws Exception {

		when(awsClient.describeStacks(any(DescribeStacksRequest.class)))
				.thenReturn(stackPendingResult(), stackPendingResult(),
						stackCompletedResult());
		assertTrue(cf.create());
		verify(awsClient, times(3)).describeStacks(
				any(DescribeStacksRequest.class));

	}

	@Test
	public void create_returns_false_when_stack_creation_fails()
			throws Exception {
		when(awsClient.describeStacks(any(DescribeStacksRequest.class)))
				.thenReturn(stackFailedResult());
		assertFalse(cf.create());
	}

	@Test
	public void delete_waits_for_stack_to_be_deleted() throws Exception {
		when(awsClient.describeStacks()).thenReturn(stackDeletingResult(),
				stackDeletingResult(), stackDeleteSuccessfulResult());
		cf.delete();
		verify(awsClient, times(3)).describeStacks();
	}

	@Test
	public void delete_returns_false_when_stack_fails_to_delete()
			throws Exception {
		when(awsClient.describeStacks()).thenReturn(stackDeleteFailedResult());
		assertFalse(cf.delete());
	}

	@Test
	public void it_should_not_execute_setEndpoint_if_awsRegion_is_not_set() {
		final AmazonCloudFormation awsClient = mock(AmazonCloudFormation.class);

		cf = new CloudFormation(System.out, TEST_STACK, recipeBody, parameters,
				-12345, awsAccessKey, awsSecretKey, null, true, new EnvVars()) {
			@Override
			protected AmazonCloudFormation getAWSClient() {
				return awsClient;
			}
		};

		verifyZeroInteractions(awsClient);
	}

	@Test
	public void it_should_not_execute_setEndpoint_if_awsRegion_is_empty() {
		final AmazonCloudFormation awsClient = mock(AmazonCloudFormation.class);

		cf = new CloudFormation(System.out, TEST_STACK, recipeBody, parameters,
				-12345, awsAccessKey, awsSecretKey, "", true, new EnvVars()) {
			@Override
			protected AmazonCloudFormation getAWSClient() {
				return awsClient;
			}
		};

		verifyZeroInteractions(awsClient);
	}

	@Test
	public void it_should_execute_setEndpoint_if_awsRegion_is_set() {
		verify(awsClient).setEndpoint("https://cloudformation.region.amazonaws.com");
	}

	private DescribeStacksResult stackDeleteFailedResult() {
		return describeStacksResultWithStatus(StackStatus.DELETE_FAILED);
	}

	private DescribeStacksResult stackDeleteSuccessfulResult() {
		return new DescribeStacksResult(); // A result with no stacks in it.
	}

	private DescribeStacksResult stackDeletingResult() {
		return describeStacksResultWithStatus(StackStatus.DELETE_IN_PROGRESS);
	}

	private DescribeStacksResult stackFailedResult() {
		return describeStacksResultWithStatus(StackStatus.CREATE_FAILED);
	}

	private CreateStackResult createResultWithId(String stackId) {
		return new CreateStackResult().withStackId(stackId);
	}

	private DescribeStacksResult stackCompletedResult() {
		return describeStacksResultWithStatus(StackStatus.CREATE_COMPLETE);
	}

	private DescribeStacksResult stackPendingResult() {
		return describeStacksResultWithStatus(StackStatus.CREATE_IN_PROGRESS);
	}

	private DescribeStacksResult describeStacksResultWithStatus(
			StackStatus status) {
		return new DescribeStacksResult().withStacks(new Stack()
				.withStackStatus(status.name()).withStackName(TEST_STACK));
	}

}
