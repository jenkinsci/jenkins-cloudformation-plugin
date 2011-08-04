package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildWrapper.Environment;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CloudFormationBuildWrapperTest {

	private CloudFormationBuildWrapper wrapper; // SUT

	@Mock private CloudFormation mockCF1;
	@Mock private CloudFormation mockCF2;
	@Mock private AbstractBuild build;
	@Mock private Launcher launcher;
	@Mock private BuildListener listener;

	private EnvVars envVars;

	@Before
	public void setUp() throws Exception {
		envVars = new EnvVars();
		when(build.getEnvironment(listener)).thenReturn(envVars);
		when(listener.getLogger()).thenReturn(System.out);
	}

	@Test
	public void when_1_stack_is_created_on_tearDown_1_stack_is_deleted()
			throws Exception {
		when_1_stack_is_entered();
		then_1_stack_is_created_and_deleted();
	}

	@Test
	public void when_2_stacks_are_entered_and_2nd_fails_to_create_first_is_deleted()
			throws Exception {
		when_2_stack_are_entered();
		and_2nd_stack_fails_to_create();
		then_first_stack_is_deleted();
	}

	private void then_first_stack_is_deleted() throws Exception {
		Environment env = wrapper.setUp(build, launcher, listener);
		verify(mockCF1, times(1)).create();
		verify(mockCF2, times(1)).create();
		env.tearDown(build, listener);
		verify(mockCF1, times(1)).delete();
	}

	private void and_2nd_stack_fails_to_create() throws Exception {
		when(mockCF1.create()).thenReturn(true);
		when(mockCF2.create()).thenReturn(false);
	}

	private void when_2_stack_are_entered() throws Exception {
		List<StackBean> stackBeans = new ArrayList<StackBean>();
		stackBeans.add(new StackBean("stack1", "stack description",
				"{resources: }", "", 0, "accessKey", "secretKey", true));
		stackBeans.add(new StackBean("stack2", "stack2 description",
				"{resources: }", "", 0, "accessKey", "secretKey", true));

		wrapper = spy(new CloudFormationBuildWrapper(stackBeans));

        when(mockCF1.getAutoDeleteStack()).thenReturn(true);
        when(mockCF2.getAutoDeleteStack()).thenReturn(true);
		
		doReturn(mockCF1).when(wrapper).newCloudFormation(
				((StackBean)argThat(hasProperty("stackName", equalTo("stack1")))),
				any(AbstractBuild.class), any(EnvVars.class),
				any(PrintStream.class));

		doReturn(mockCF2).when(wrapper).newCloudFormation(
				((StackBean)argThat(hasProperty("stackName", equalTo("stack2")))),
				any(AbstractBuild.class), any(EnvVars.class),
				any(PrintStream.class));

	}

	private void then_1_stack_is_created_and_deleted() throws Exception {
		Environment env = wrapper.setUp(build, launcher, listener);
		verify(mockCF1, times(1)).create();
		env.tearDown(build, listener);
		verify(mockCF1, times(1)).delete();
	}

	private void when_1_stack_is_entered() throws Exception {
		List<StackBean> stackBeans = new ArrayList<StackBean>();
		stackBeans.add(new StackBean("stack1", "stack description",
				"{resources: }", "", 0, "accessKey", "secretKey", true));

		wrapper = spy(new CloudFormationBuildWrapper(stackBeans));

        when(mockCF1.getAutoDeleteStack()).thenReturn(true);

        doReturn(mockCF1).when(wrapper).newCloudFormation(any(StackBean.class),
				any(AbstractBuild.class), any(EnvVars.class),
				any(PrintStream.class));

		when(mockCF1.create()).thenReturn(true);
		when(mockCF1.delete()).thenReturn(true);
	}

}
