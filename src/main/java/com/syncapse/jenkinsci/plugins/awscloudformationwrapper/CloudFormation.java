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
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.retry.RetryUtils;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ListStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.StackSummary;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.google.common.collect.Lists;
import hudson.EnvVars;
import hudson.ProxyConfiguration;
import hudson.model.Hudson;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Class for interacting with CloudFormation stacks, including creating them,
 * deleting them and getting the outputs.
 *
 * @author erickdovale
 *
 */
public class CloudFormation {

    private static final int COMPLETE_OPERATION_WAITING_TIME_MS = 10000;
    private AmazonCloudFormationClient client;
    /**
     * Minimum time to wait before considering the creation of the stack a
     * failure. Default value is 5 minutes. (300 seconds)
     */
    public static final long MIN_TIMEOUT = 300;
    private String stackName;
    private Boolean isRecipeURL;
    private String recipe;
    private List<Parameter> parameters;
    private long timeout;
    private String awsAccessKey;
    private String awsSecretKey;
    private PrintStream logger;
    private AmazonCloudFormation amazonClient;
    private Stack stack;
    private boolean autoDeleteStack;
    private EnvVars envVars;
    private Region awsRegion;
    private Boolean isPrefixSelected;
    private Boolean isTagFilterOn;
    private Map<String, String> outputs;
    private long sleep=0;

    /**
     * @param logger a logger to write progress information.
     * @param stackName the name of the stack as defined in the AWS
     * CloudFormation API.
     * @param isRecipeURL true to treat the recipeBody as a URL
     * @param recipeBody the body of the json document describing the stack.
     * @param parameters a Map of where the keys are the param name and the
     * value the param value.
     * @param timeout Time to wait for the creation of a stack to complete. This
     * value will be the greater between {@link #MIN_TIMEOUT} and the given
     * value.
     * @param awsAccessKey the AWS API Access Key.
     * @param awsSecretKey the AWS API Secret Key.
     */
    public CloudFormation(PrintStream logger, String stackName, Boolean isRecipeURL,
            String recipeBody, Map<String, String> parameters,
            long timeout, String awsAccessKey, String awsSecretKey, Region region,
            boolean autoDeleteStack, EnvVars envVars, Boolean isPrefixSelected) {

        this.logger = logger;
        this.stackName = stackName;
        this.isRecipeURL = isRecipeURL;
        this.recipe = recipeBody;
        this.parameters = parameters(parameters);
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
        this.awsRegion = region != null ? region : Region.getDefault();
        this.isPrefixSelected = isPrefixSelected;
        
        if (timeout == -12345) {
            this.timeout = 0; // Faster testing.
        } else {
            this.timeout = timeout > MIN_TIMEOUT ? timeout : MIN_TIMEOUT;
        }
        this.amazonClient = getAWSClient();
        this.autoDeleteStack = autoDeleteStack;
        this.envVars = envVars;
    
    }
    public CloudFormation(PrintStream logger, String stackName, Boolean isRecipeURL,
            String recipeBody, Map<String, String> parameters,
            long timeout, String awsAccessKey, String awsSecretKey, Region region,
            EnvVars envVars, Boolean isPrefixSelected,long sleep) {

        this.logger = logger;
        this.stackName = stackName;
        this.isRecipeURL = isRecipeURL;
        this.recipe = recipeBody;
        this.parameters = parameters(parameters);
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
        this.awsRegion = region != null ? region : Region.getDefault();
        this.isPrefixSelected = isPrefixSelected;
        if (timeout == -12345) {
            this.timeout = 0; // Faster testing.
        } else {
            this.timeout = timeout > MIN_TIMEOUT ? timeout : MIN_TIMEOUT;
        }
        this.amazonClient = getAWSClient();
        this.autoDeleteStack = false;
        this.envVars = envVars;
        this.sleep=sleep;
    
    }
    public CloudFormation(PrintStream logger, String stackName, Boolean isRecipeURL,
            String recipeBody, Map<String, String> parameters, long timeout,
            String awsAccessKey, String awsSecretKey, boolean autoDeleteStack,
            EnvVars envVars, Boolean isPrefixSelected) {
        this(logger, stackName, isRecipeURL, recipeBody, parameters, timeout, awsAccessKey,
                awsSecretKey, null, autoDeleteStack, envVars, isPrefixSelected);
    }

    /**
     * Return true if this stack should be automatically deleted at the end of
     * the job, or false if it should not be automatically deleted.
     *
     * @return true if this stack should be automatically deleted at the end of
     * the job, or false if it should not be automatically deleted.
     */
    public boolean getAutoDeleteStack() {
        return autoDeleteStack;
    }

    /**
     * @return
     */
    public boolean delete() {
        if (isPrefixSelected) {
            stackName = getOldestStackNameWithPrefix();
        }
        logger.println("Deleting Cloud Formation stack: " + getExpandedStackName());
        DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
        deleteStackRequest.withStackName(getExpandedStackName());
        amazonClient.deleteStack(deleteStackRequest);
        boolean result = waitForStackToBeDeleted();

        logger.println("Cloud Formation stack: " + getExpandedStackName()
                + (result ? " deleted successfully" : " failed deleting."));
        return result;
    }

    /**
     * @return True of the stack was created successfully. False otherwise.
     *
     * @throws TimeoutException if creating the stack takes longer than the
     * timeout value passed during creation.
     *
     * @see CloudFormation#CloudFormation(PrintStream, String, String, Map,
     * long, String, String)
     */
    public boolean create() throws TimeoutException, InterruptedException {

        logger.println("Determining to create or update Cloud Formation stack: " + getExpandedStackName());

        try {
            try {
                DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(getExpandedStackName());
                Stack stack = getStack(amazonClient.describeStacks(describeStacksRequest));
            } catch (AmazonServiceException e) {
                logger.println("Stack not found: " + getExpandedStackName() + ". Reason: " + detailedError(e));
            } catch (AmazonClientException e) {
                logger.println("Stack not found: " + getExpandedStackName() + ". Error was: " + e.getCause());
            }

            if(stack == null) {
                logger.println("Creating Cloud Formation stack: " + getExpandedStackName());
                CreateStackRequest request = createStackRequest();
                amazonClient.createStack(request);
            }
            else {
                logger.println("Updating Cloud Formation stack: " + getExpandedStackName());
                UpdateStackRequest updateRequest = updateStackRequest();
                amazonClient.updateStack(updateRequest);
            }


            stack = waitForStackToBeCreated();

            StackStatus status = getStackStatus(stack.getStackStatus());

            Map<String, String> stackOutput = new HashMap<String, String>();
            if (isStackCreationSuccessful(status)) {
                List<Output> outputs = stack.getOutputs();
                for (Output output : outputs) {
                    stackOutput.put(output.getOutputKey(), output.getOutputValue());
                }

                logger.println("Successfully created stack: " + getExpandedStackName());
                this.outputs = stackOutput;
                Thread.sleep(TimeUnit.SECONDS.toMillis(sleep));
                return true;
            } else {
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

    private String detailedError(AmazonServiceException e) {
        StringBuffer message = new StringBuffer();
        message.append("Detailed Message: ").append(e.getMessage()).append('\n');
        message.append("Status Code: ").append(e.getStatusCode()).append('\n');
        message.append("Error Code: ").append(e.getErrorCode()).append('\n');
        return message.toString();
    }

    protected AmazonCloudFormation getAWSClient() {
        AWSCredentials credentials = new BasicAWSCredentials(this.awsAccessKey,
                this.awsSecretKey);
        Hudson hudson = Hudson.getInstance(); 
        ProxyConfiguration proxyConfig = hudson != null ? hudson.proxy : null;
        if (proxyConfig != null && proxyConfig.name != null) {
           ClientConfiguration config = new ClientConfiguration();
           config.setProxyHost(proxyConfig.name);
           config.setProxyPort(proxyConfig.port);
           config.setProxyUsername(proxyConfig.getUserName());
           config.setProxyPassword(proxyConfig.getPassword());
           config.setPreemptiveBasicProxyAuth(true);
           AWSCredentialsProvider provider = new BasicAWSCredentialsProvider(credentials);
           AmazonCloudFormation amazonClient = new AmazonCloudFormationAsyncClient(
                   provider, config);
   
           amazonClient.setEndpoint(awsRegion.endPoint);
           return amazonClient;
        } else {
          AmazonCloudFormation amazonClient = new AmazonCloudFormationAsyncClient(
                  credentials);
          amazonClient.setEndpoint(awsRegion.endPoint);
          return amazonClient;
        }
    }

    private boolean waitForStackToBeDeleted() {
        int retries = 1;
        while (true) {
            try {

              stack = getStack(amazonClient.describeStacks());

              if (stack == null) {
                  return true;
              }

              StackStatus stackStatus = getStackStatus(stack.getStackStatus());

              if (StackStatus.DELETE_COMPLETE == stackStatus) {
                  return true;
              }

              if (StackStatus.DELETE_FAILED == stackStatus) {
                  return false;
              }

              logger.println("Stack status " + stackStatus + ".");
              
            } catch (AmazonServiceException ase) {
                if (!RetryUtils.isThrottlingException(ase)) {
                    throw ase;
                }
            }
            sleep(retries);
            retries++;
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

    private Stack waitForStackToBeCreated() throws TimeoutException {

        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(getExpandedStackName());
        StackStatus status = StackStatus.CREATE_IN_PROGRESS;
        Stack stack = null;
        long startTime = System.currentTimeMillis();
        int retries = 1;
        long subTime = startTime, lastTime;
        while (isStackCreationInProgress(status)) {
            lastTime = subTime;
            subTime = System.currentTimeMillis();
            try {
                stack = getStack(amazonClient.describeStacks(describeStacksRequest));
                status = getStackStatus(stack.getStackStatus());
                logger.println("Stack status " + status + ". ( " + (subTime - lastTime) + "ms since previous check)");
                if (isStackCreationInProgress(status)) {
                    if (isTimeout(startTime)) {
                        throw new TimeoutException("Timed out waiting for stack to be created. (timeout=" + timeout + ")");
                    }
                    sleep(retries);
                }
            } catch (AmazonServiceException ase) {
                if (!RetryUtils.isThrottlingException(ase)) {
                    throw ase;
                }
                if (isTimeout(startTime)) {
                    throw new TimeoutException("Timed out waiting for stack to be created. (timeout=" + timeout + ")");
                }
                logger.println("Stack status request throttled; retrying.");
                sleep(retries);
            }
            retries++;
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
        for (Stack aStack : result.getStacks()) {
            if (getExpandedStackName().equals(aStack.getStackName())) {
                return aStack;
            }
        }

        return null;

    }

    private boolean isStackCreationSuccessful(StackStatus status) {
        return status == StackStatus.CREATE_COMPLETE || status == StackStatus.UPDATE_COMPLETE;
    }

    private long getWaitBetweenAttempts (int retries) {
        if (timeout == 0) {
            return 0;
        } else {
            return (long) Math.min(Math.pow(2, retries) * 100L, 300000);
        }
    }

    private void sleep(int retries) {
        try {
            Thread.sleep(getWaitBetweenAttempts(retries));
        } catch (InterruptedException e) {
            if (stack != null) {
                logger.println("Received an interruption signal. There is a stack created or in the proces of creation. Check in your amazon account to ensure you are not charged for this.");
                logger.println("Stack details: " + stack);
            }
        }
    }

    private boolean isStackCreationInProgress(StackStatus status) {
        return status == StackStatus.CREATE_IN_PROGRESS || status == StackStatus.UPDATE_IN_PROGRESS;
    }

    private StackStatus getStackStatus(String status) {
        StackStatus result = StackStatus.fromValue(status);
        return result;
    }

    private CreateStackRequest createStackRequest() {

        CreateStackRequest r = new CreateStackRequest();
        r.withStackName(getExpandedStackName());
        r.withParameters(parameters);
        if (isRecipeURL) {
            r.withTemplateURL(recipe);
        } else {
            r.withTemplateBody(recipe);
        }
        r.withCapabilities("CAPABILITY_IAM");

        return r;
    }

    private UpdateStackRequest updateStackRequest() {
		UpdateStackRequest r = new UpdateStackRequest();
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

    private String getOldestStackNameWithPrefix() {
        List<StackSummary> stackSummaries = getAllRunningStacks();
        ArrayList<StackSummary> filteredStackSummries = new ArrayList<StackSummary>();
        List<String> stackNames = new ArrayList<String>();
        for (StackSummary summary : stackSummaries) {
            if (summary.getStackName().startsWith(stackName)) {
                filteredStackSummries.add(summary);
            }
        }
        if (filteredStackSummries.isEmpty() || filteredStackSummries.size() == 1) {
            return stackName;
        }
        return returnOldestStackName(filteredStackSummries);

    }

    private String returnOldestStackName(ArrayList<StackSummary> filteredStackSummries) {
        Date date = filteredStackSummries.get(0).getCreationTime();
        String stackToDelete = "";
        for (StackSummary summary : filteredStackSummries) {
            if (summary.getCreationTime().before(date));
            {
                date = summary.getCreationTime();
                stackToDelete = summary.getStackName();
            }
        }
        
        return stackToDelete;
    }

    private List<StackSummary> getAllRunningStacks() {
        client = new AmazonCloudFormationClient(new AWSCredentials() {
            public String getAWSAccessKeyId() {
                return awsAccessKey;
            }

            public String getAWSSecretKey() {
                return awsSecretKey;
            }
        });
        List<String> stackStatusFilters = new ArrayList<String>();
        stackStatusFilters.add("UPDATE_COMPLETE");
        stackStatusFilters.add("CREATE_COMPLETE");
        stackStatusFilters.add("ROLLBACK_COMPLETE");
        ListStacksRequest listStacksRequest = new ListStacksRequest();
        listStacksRequest.setStackStatusFilters(stackStatusFilters);
        ListStacksResult result = client.listStacks(listStacksRequest);
        List<StackSummary> stackSummaries = result.getStackSummaries();
        return stackSummaries;
    }

    public Map<String, String> getStackParameters(String stackName) {
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest();
        describeStacksRequest.setStackName(stackName);
        DescribeStacksResult describeStacksResult = client
                .describeStacks(describeStacksRequest);
        List<Stack> stacks = describeStacksResult.getStacks();
        Stack stack = stacks.get(0);
        List<Parameter> parameters = stack.getParameters();
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (Parameter parameter : parameters) {
            map.put(parameter.getParameterKey(), parameter.getParameterValue());
        }
        return map;
    }

    public static boolean isRecipeURL(String recipe) {
        // if the recipe name begins with http:// or https:// then treat as a URL
        // ...didn't want to catch files that start with http
        if(recipe.regionMatches(true, 0, "http://", 0, 7)
            || recipe.regionMatches(true, 0, "https://", 0, 8)) {
            return true;
        }
        return false;
    }
}

class BasicAWSCredentialsProvider implements AWSCredentialsProvider {
   
   AWSCredentials awsCredentials;
   
   public BasicAWSCredentialsProvider(AWSCredentials awsCredentials) {
       this.awsCredentials = awsCredentials;
   }
   
   public AWSCredentials getCredentials() {
       return awsCredentials;
   }
   
   public void refresh() {
       
   }
   
}
