/**
 * 
 */
package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
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
import hudson.EnvVars;

/**
 * Class for interacting with CloudFormation stacks, including creating them, deleting them and getting the outputs.
 * @author erickdovale
 * 
 */
public class CloudFormation {
	
	/**
	 * Minimum time to wait before considering the creation of the stack a failure. 
	 * Default value is 5 minutes. (300 seconds)
	 */
	public static final long MIN_TIMEOUT = 300;

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
    private boolean autoDeleteStack;
	private EnvVars envVars;

	private Map<String, String> outputs;

	/**
	 * @param logger a logger to write progress information.
	 * @param stackName the name of the stack as defined in the AWS CloudFormation API.
	 * @param recipeBody the body of the json document describing the stack.
	 * @param parameters a Map of where the keys are the param name and the value the param value.
	 * @param timeout Time to wait for the creation of a stack to complete. This value will be the greater between {@link #MIN_TIMEOUT} and the given value.
	 * @param awsAccessKey the AWS API Access Key.
	 * @param awsSecretKey the AWS API Secret Key.
	 */
	public CloudFormation(PrintStream logger, String stackName,
			String recipeBody, Map<String, String> parameters,
			long timeout, String awsAccessKey, String awsSecretKey,
            boolean autoDeleteStack, EnvVars envVars) {

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
        this.autoDeleteStack = autoDeleteStack;
		this.envVars = envVars;
	}

    /**
     * Return true if this stack should be automatically deleted at the end of the job, or false if it should not
     * be automatically deleted.
     * @return true if this stack should be automatically deleted at the end of the job, or false if it should not
     * be automatically deleted.
     */
    public boolean getAutoDeleteStack() {
        return autoDeleteStack;
    }
	
	/**
	 * @return
	 */
	public boolean delete() {
		logger.println("Deleting Cloud Formation stack: " + getExpandedStackName());
		
		DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
		deleteStackRequest.withStackName(getExpandedStackName());
		
		amazonClient.deleteStack(deleteStackRequest);
		boolean result = waitForStackToBeDeleted();
		
		logger.println("Cloud Formation stack: " + getExpandedStackName()
				+ (result ? " deleted successfully" : " failed deleting.") );
		return result;
	}

	/**
	 * @return True of the stack was created successfully. False otherwise.
	 * 
	 * @throws TimeoutException if creating the stack takes longer than the timeout value passed during creation.
	 * 
	 * @see CloudFormation#CloudFormation(PrintStream, String, String, Map, long, String, String)
	 */
	public boolean create() throws TimeoutException {

		logger.println("Creating Cloud Formation stack: " + getExpandedStackName());
		
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
				
				logger.println("Successfully created stack: " + getExpandedStackName());
				
				this.outputs = stackOutput;
				return true;
			} else{
				logger.println("Failed to create stack: " + getExpandedStackName() + ". Reason: " + stack.getStackStatusReason());
				return false;
			}
		} catch (AmazonServiceException e) {
			logger.println("Failed to create stack: " + getExpandedStackName() + ". Reason: " + detailedError(e));
			return false;
		} catch (AmazonClientException e) {
			logger.println("Failed to create stack: " + getExpandedStackName() + ". Error was: " + e.getCause());
			return false;
		}

	}
	
	private String detailedError(AmazonServiceException e){
		StringBuffer message = new StringBuffer();
		message.append("Detailed Message: ").append(e.getMessage()).append('\n');
		message.append("Status Code: ").append(e.getStatusCode()).append('\n');
		message.append("Error Code: ").append(e.getErrorCode()).append('\n');
		return message.toString();
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

	private Stack waitForStackToBeCreated() throws TimeoutException{
		
		DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(getExpandedStackName());
		StackStatus status = StackStatus.CREATE_IN_PROGRESS;
		Stack stack = null;
		long startTime = System.currentTimeMillis();
		while ( isStackCreationInProgress(status) ){
			if (isTimeout(startTime)){
				throw new TimeoutException("Timed out waiting for stack to be created. (timeout=" + timeout + ")");
			}
			stack = getStack(amazonClient.describeStacks(describeStacksRequest));
			status = getStackStatus(stack.getStackStatus());
			if (isStackCreationInProgress(status)) sleep();
		}
		
		printStackEvents();
		
		return stack;
	}

	private void printStackEvents() {
		DescribeStackEventsRequest r = new DescribeStackEventsRequest();
		r.withStackName(getExpandedStackName());
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
			if (getExpandedStackName().equals(aStack.getStackName())){
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

	private CreateStackRequest createStackRequest() {

		CreateStackRequest r = new CreateStackRequest();
		r.withStackName(getExpandedStackName());
		r.withParameters(parameters);
		r.withTemplateBody(recipe);
		r.withCapabilities("CAPABILITY_IAM");
		
		return r;
	}

	public Map<String, String> getOutputs() {
		// Prefix outputs with stack name to prevent collisions with other stacks created in the same build.
		HashMap<String, String> map = new HashMap<String, String>();
		for (String key : outputs.keySet()) {
			map.put(getExpandedStackName() + "_" + key, outputs.get(key));
		}
		return map;
	}

	private String getExpandedStackName() {
		return envVars.expand(stackName);
	}
}
