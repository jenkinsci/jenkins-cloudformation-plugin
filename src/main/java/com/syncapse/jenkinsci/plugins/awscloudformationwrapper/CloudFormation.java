/**
 * 
 */
package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.google.common.collect.Lists;

/**
 * @author erickdovale
 * 
 */
public class CloudFormation {
	
	/**
	 * Minimum time to wait before considering the creation of the stack a failure. 
	 * Default value is 5 minutes. (300 seconds)
	 */
	private static final long MIN_TIMEOUT = 300;

	private String stackName;
	private String recipe;
	private List<Parameter> parameters;
	private long timeout;
	private String awsAccessKey;
	private String awsSecretKey;
	private PrintStream logger;
	private AmazonCloudFormation amazonClient;
	private Stack stack;
	private long waitBetweenAttempts;

	private Map<String, String> outputs;

	public CloudFormation(PrintStream logger, String stackName,
			String recipeBody, Map<String, String> parameters,
			long timeout, String awsAccessKey, String awsSecretKey) {

		this.stackName = stackName;
		this.recipe = recipeBody;
		this.parameters = parameters(parameters);
		this.awsAccessKey = awsAccessKey;
		this.awsSecretKey = awsSecretKey;
		if (timeout == -12345){
			this.timeout = 0; // Faster testing.
			this.waitBetweenAttempts = 0;
		} else{
			this.timeout = timeout > MIN_TIMEOUT ? timeout : MIN_TIMEOUT;
			this.waitBetweenAttempts = 10; // query every 10s
		}
		this.logger = logger;
		this.amazonClient = getAWSClient();
		
	}
	
	/**
	 * @return
	 */
	public boolean delete() {
		logger.println("Deleting Cloud Formation stack: " + stackName);
		
		DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
		deleteStackRequest.withStackName(stackName);
		
		amazonClient.deleteStack(deleteStackRequest);
		boolean result = waitForStackToBeDeleted();
		
		logger.println("Cloud Formation stack: " + stackName
				+ (result ? " deleted successfully" : " failed deleting.") );
		return result;
	}

	/**
	 * @return A Map containing all outputs or null if creating the stack fails.
	 * 
	 * @throws IOException
	 */
	public boolean create() throws IOException {

		logger.println("Creating Cloud Formation stack: " + stackName);
		
		CreateStackRequest request = createStackRequest();
		
		try {
			amazonClient.createStack(request);
			stack = waitForStackToBeCreated();
			
			StackStatus status = getStackStatus(stack.getStackStatus());
			
			Map<String, String> stackOutput = new HashMap<String, String>();
			if (isStackCreationSuccessful(status)){
				List<Output> outputs = stack.getOutputs();
				for (Output output : outputs){
					stackOutput.put(output.getOutputKey(), output.getOutputValue());
				}
				
				logger.println("Successfully created stack: " + stackName);
				
				this.outputs = stackOutput;
				return true;
			} else{
				logger.println("Failed to create stack: " + stackName + ". Reason: " + stack.getStackStatusReason());
				return false;
			}
		} catch (AmazonClientException e) {
			logger.println("Failed to create stack: " + stackName + ". Reason: " + e.getLocalizedMessage());
			return false;
		}

	}

	protected AmazonCloudFormation getAWSClient() {
		AWSCredentials credentials = new BasicAWSCredentials(this.awsAccessKey,
				this.awsSecretKey);
		AmazonCloudFormation amazonClient = new AmazonCloudFormationAsyncClient(
				credentials);
		return amazonClient;
	}
	
	private boolean waitForStackToBeDeleted() {
		
		while (true){
			
			stack = getStack(amazonClient.describeStacks());
			
			if (stack == null) return true;
			
			StackStatus stackStatus = getStackStatus(stack.getStackStatus());
			
			if (StackStatus.DELETE_COMPLETE == stackStatus) return true;
				
			if (StackStatus.DELETE_FAILED == stackStatus) return false;
			
			sleep();
			
		}
		
	}

	private List<Parameter> parameters(Map<String, String> parameters) {
	
		if (parameters == null || parameters.values().size() == 0) {
			return null;
		}
	
		List<Parameter> result = Lists.newArrayList();
		Parameter parameter = null;
		for (String name : parameters.keySet()) {
			parameter = new Parameter();
			parameter.setParameterKey(name);
			parameter.setParameterValue(parameters.get(name));
			result.add(parameter);
		}
	
		return result;
	}

	private Stack waitForStackToBeCreated() {
		DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackName);
		StackStatus status = StackStatus.CREATE_IN_PROGRESS;
		Stack stack = null;
		long startTime = System.currentTimeMillis();
		while ( isStackCreationInProgress(status) && !isTimeout(startTime)){
			stack = getStack(amazonClient.describeStacks(describeStacksRequest));
			status = getStackStatus(stack.getStackStatus());
			if (isStackCreationInProgress(status)) sleep();
		}
		
		printStackEvents();
		
		return stack;
	}

	private void printStackEvents() {
		DescribeStackEventsRequest r = new DescribeStackEventsRequest();
		r.withStackName(stackName);
		DescribeStackEventsResult describeStackEvents = amazonClient.describeStackEvents(r);
		
		List<StackEvent> stackEvents = describeStackEvents.getStackEvents();
		Collections.reverse(stackEvents);
		
		for (StackEvent event : stackEvents) {
			logger.println(event.getEventId() + " - " + event.getResourceType() + " - " + event.getResourceStatus() + " - " + event.getResourceStatusReason());
		}
		
	}

	private boolean isTimeout(long startTime) {
		return timeout == 0 ? false : (System.currentTimeMillis() - startTime) > (timeout * 1000);
	}

	private Stack getStack(DescribeStacksResult result) {
		for (Stack aStack : result.getStacks())
			if (stackName.equals(aStack.getStackName())){
				return aStack;
			}
		
		return null;
		
	}

	private boolean isStackCreationSuccessful(StackStatus status) {
		return status == StackStatus.CREATE_COMPLETE;
	}

	private void sleep() {
		try {
			Thread.sleep(waitBetweenAttempts * 1000);
		} catch (InterruptedException e) {
			if (stack != null){
				logger.println("Received an interruption signal. There is a stack created or in the proces of creation. Check in your amazon account to ensure you are not charged for this.");
				logger.println("Stack details: " + stack);
			}
		}
	}

	private boolean isStackCreationInProgress(StackStatus status) {
		return status == StackStatus.CREATE_IN_PROGRESS;
	}

	private StackStatus getStackStatus(String status) {
		StackStatus result = StackStatus.fromValue(status);
		return result;
	}

	private CreateStackRequest createStackRequest() throws IOException {

		CreateStackRequest r = new CreateStackRequest();
		r.withStackName(stackName);
		r.withParameters(parameters);
		r.withTemplateBody(recipe);

		return r;
	}

	public Map<String, String> getOutputs() {
		// Prefix outputs with stack name to prevent collisions with other stacks created in the same build.
		HashMap<String, String> map = new HashMap<String, String>();
		for (String key : outputs.keySet()) {
			map.put(stackName + "_" + key, outputs.get(key));
		}
		return map;
	}

}
