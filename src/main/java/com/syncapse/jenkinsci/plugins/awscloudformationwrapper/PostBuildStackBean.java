package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 *
 *
 * @author erickdovale
 *
 */
public class PostBuildStackBean extends AbstractDescribableImpl<PostBuildStackBean> {

	/**
	 * The name of the stack.
	 */
	private String stackName;

	/**
	 * The description of the cloud formation stack that will be launched.
	 */
	private String description;

	/**
	 * The json file with the Cloud Formation definition.
	 */
	private String cloudFormationRecipe;

	/**
	 * The parameters to be passed into the cloud formation.
	 */
	private String parameters;

	/**
	 * Time to wait for a stack to be created before giving up and failing the build.
	 */
	private long timeout;

	/**
	 * Number of seconds to wait before checking with AWS for stack progress
	 */
	private long checkInterval;

	/**
	 * The access key to call Amazon's APIs
	 */
	private String awsAccessKey;

	/**
	 * The secret key to call Amazon's APIs
	 */
	private String awsSecretKey;


        private long sleep;


    private Region awsRegion;

	@DataBoundConstructor
	public PostBuildStackBean(String stackName, String description,
			String cloudFormationRecipe, String parameters, long timeout,
			String awsAccessKey, String awsSecretKey, Region awsRegion,long sleep, long checkInterval) {
		super();
		this.stackName = stackName;
		this.description = description;
		this.cloudFormationRecipe = cloudFormationRecipe;
		this.parameters = parameters;
		this.timeout = timeout;
		this.awsAccessKey = awsAccessKey;
		this.awsSecretKey = awsSecretKey;
		this.checkInterval = checkInterval;
    this.sleep=sleep;
    this.awsRegion = awsRegion;
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

	public long getTimeout() {
		return timeout;
	}

	public long getCheckInterval() {
		return checkInterval;
	}


	public String getAwsAccessKey() {
		return awsAccessKey;
	}

	public String getAwsSecretKey() {
		return awsSecretKey;
	}
        public long getSleep() {
		return sleep;
	}


    public Region getAwsRegion(){
    	return awsRegion;
    }

	public Map<String, String> getParsedParameters(EnvVars env) {

		if (parameters == null || parameters.isEmpty())
			return new HashMap<String, String>();

		Map<String, String> result = new HashMap<String, String>();
		String token[] = null;

		//semicolon delimited list
		if(parameters.contains(";")) {
			for (String param : parameters.split(";")) {
				token = param.split("=");
				result.put(token[0].trim(), env.expand(token[1].trim()));
			}
		} else {
			//comma delimited parameter list
			for (String param : parameters.split(",")) {
				token = param.split("=");
				result.put(token[0].trim(), env.expand(token[1].trim()));
			}
		}
		return result;
	}

	public String getParsedAwsAccessKey(EnvVars env) {
		return env.expand(getAwsAccessKey());
	}


	public String getParsedAwsSecretKey(EnvVars env) {
		return env.expand(getAwsSecretKey());
	}

	@Extension
	public static final class DescriptorImpl extends Descriptor<PostBuildStackBean>{

		@Override
		public String getDisplayName() {
			return "Cloud Formation";
		}

        public FormValidation doCheckStackName(
				@AncestorInPath AbstractProject<?, ?> project,
				@QueryParameter String value) throws IOException {
			if (0 == value.length()) {
				return FormValidation.error("Empty stack name");
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckTimeout(
				@AncestorInPath AbstractProject<?, ?> project,
				@QueryParameter String value) throws IOException {
			if (value.length() > 0) {
				try {
					Long.parseLong(value);
				} catch (NumberFormatException e) {
					return FormValidation.error("Timeout value "+ value + " is not a number.");
				}
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckCheckInterval(
				@AncestorInPath AbstractProject<?, ?> project,
				@QueryParameter String value) throws IOException {
			if (value.length() > 0) {
				try {
					Long.parseLong(value);
				} catch (NumberFormatException e) {
					return FormValidation.error("Timeout value "+ value + " is not a number.");
				}
			}
			return FormValidation.ok();
		}

	public FormValidation doCheckSleep(
				@AncestorInPath AbstractProject<?, ?> project,
				@QueryParameter String value) throws IOException {
			if (value.length() > 0) {
				try {
					Long.parseLong(value);
				} catch (NumberFormatException e) {
					return FormValidation.error("Timeout value "+ value + " is not a number.");
				}
			}
			return FormValidation.ok();
		}
		public FormValidation doCheckCloudFormationRecipe(
				@AncestorInPath AbstractProject<?, ?> project,
				@QueryParameter String value) throws IOException {
			if (0 == value.length()) {
				return FormValidation.error("Empty recipe file.");
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckAwsAccessKey(
				@AncestorInPath AbstractProject<?, ?> project,
				@QueryParameter String value) throws IOException {
			if (0 == value.length()) {
				return FormValidation.error("Empty aws access key");
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckAwsSecretKey(
				@AncestorInPath AbstractProject<?, ?> project,
				@QueryParameter String value) throws IOException {
			if (0 == value.length()) {
				return FormValidation.error("Empty aws secret key");
			}
			return FormValidation.ok();
		}

		public ListBoxModel doFillAwsRegionItems() {
            ListBoxModel items = new ListBoxModel();
            for (Region region : Region.values()) {
				items.add(region.readableName, region.name());
			}
            return items;
        }

	}


}