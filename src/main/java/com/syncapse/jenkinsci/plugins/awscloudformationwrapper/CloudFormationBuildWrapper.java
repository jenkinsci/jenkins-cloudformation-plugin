/**
 * 
 */
package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * @author erickdovale
 * 
 */
public class CloudFormationBuildWrapper extends BuildWrapper {

	/**
	 * Minimum time to wait before considering the creation of the stack a failure. 
	 * Default value is 5 minutes. (300 seconds)
	 */
	private static final long MIN_TIMEOUT = 300;

	/**
	 * The name of the stack.
	 */
	private final String stackName;

	/**
	 * The description of the cloud formation stack that will be launched.
	 */
	private final String description;

	/**
	 * The json file with the Cloud Formation definition.
	 */
	private final String cloudFormationRecipe;

	/**
	 * The parameters to be passed into the cloud formation.
	 */
	private final String parameters;
	
	/**
	 * Time to wait for a stack to be created before giving up and failing the build. 
	 */
	private final long timeout;

	/**
	 * The access key to call Amazon's APIs
	 */
	private final String awsAccessKey;

	/**
	 * The secret key to call Amazon's APIs
	 */
	private final String awsSecretKey;

	protected Map<String, String> parsedParameters;

	@DataBoundConstructor
	public CloudFormationBuildWrapper(String stackName, String description,
			String cloudFormationRecipe, String parameters, String timeout, 
			String awsAccessKey, String awsSecretKey) {

		this.stackName = stackName;
		this.description = description;
		this.cloudFormationRecipe = cloudFormationRecipe;
		this.parameters = parameters;
		this.timeout = Long.parseLong(timeout) > MIN_TIMEOUT ? Long.parseLong(timeout) : MIN_TIMEOUT;
		this.awsAccessKey = awsAccessKey;
		this.awsSecretKey = awsSecretKey;
		this.parsedParameters = parseParameters(parameters);

	}

	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws IOException, InterruptedException {

		final CloudFormation cloudFormation = newCloudFormation(build, listener.getLogger());

		final Map<String, String> stackOutputs = cloudFormation.create();

		if (stackOutputs == null) {
			build.setResult(Result.FAILURE);
			return null;
		}

		return new Environment() {
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener)
					throws IOException, InterruptedException {

				return cloudFormation.delete();

			}

			@Override
			public void buildEnvVars(Map<String, String> env) {
				env.putAll(stackOutputs);
			}
		};
	}

	protected CloudFormation newCloudFormation(AbstractBuild build,
			PrintStream logger) throws IOException {
		return new CloudFormation(logger, stackName,
				cloudFormationRecipe(build), parsedParameters, timeout, awsAccessKey,
				awsSecretKey);
	}

	private String cloudFormationRecipe(AbstractBuild build) throws IOException {
		return build.getWorkspace().child(cloudFormationRecipe).readToString();
	}
	
	/**
	 * 
	 * @param params a comma separated list of key/value pairs. eg: key1=value1,key2=value2
	 * @return
	 */
	private Map<String, String> parseParameters(String params) {
		
		if (params == null || params.isEmpty())
			return new HashMap<String, String>();
		
		Map<String, String> result = new HashMap<String, String>();
		String token[] = null;
		for (String param : params.split(",")) {
			token = param.split("=");
			result.put(token[0].trim(), token[1].trim());
		}
		
		return result;
	}

	@Extension
	public static class DescriptorImpl extends BuildWrapperDescriptor {
		public DescriptorImpl() {
			super(CloudFormationBuildWrapper.class);
		}

		public FormValidation doCheckStackName(
				@AncestorInPath AbstractProject project,
				@QueryParameter String value) throws IOException {
			if (0 == value.length()) {
				return FormValidation.error("Empty stack name");
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckTimeout(
				@AncestorInPath AbstractProject project,
				@QueryParameter String value) throws IOException {
			if (value.length() > 0) {
				try {
					long time = Long.parseLong(value);
				} catch (NumberFormatException e) {
					return FormValidation.error("Timeout value "+ value + " is not a number.");
				}
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckCloudFormationRecipe(
				@AncestorInPath AbstractProject project,
				@QueryParameter String value) throws IOException {
			if (0 == value.length()) {
				return FormValidation.error("Empty recipe file.");
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckAwsAccessKey(
				@AncestorInPath AbstractProject project,
				@QueryParameter String value) throws IOException {
			if (0 == value.length()) {
				return FormValidation.error("Empty aws access key");
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckAwsSecretKey(
				@AncestorInPath AbstractProject project,
				@QueryParameter String value) throws IOException {
			if (0 == value.length()) {
				return FormValidation.error("Empty aws secret key");
			}
			return FormValidation.ok();
		}

		@Override
		public String getDisplayName() {
			return "Create AWS Cloud Formation stack";
		}

		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return true;
		}

	}

	public String getStackName() {
		return stackName;
	}

	public String getDescription() {
		return description;
	}

	public String getCloudFormationRecipe() {
		return cloudFormationRecipe;
	}

	public String getParameters() {
		return parameters;
	}

	public String getAwsAccessKey() {
		return awsAccessKey;
	}

	public String getAwsSecretKey() {
		return awsSecretKey;
	}

	public long getTimeout() {
		return timeout;
	}

}
