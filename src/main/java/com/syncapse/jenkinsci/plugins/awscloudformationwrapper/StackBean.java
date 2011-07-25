package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

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
public class StackBean extends AbstractDescribableImpl<StackBean> {
	
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
	 * The access key to call Amazon's APIs
	 */
	private String awsAccessKey;
	
	/**
	 * The secret key to call Amazon's APIs
	 */
	private String awsSecretKey;
	
	@DataBoundConstructor
	public StackBean(String stackName, String description,
			String cloudFormationRecipe, String parameters, long timeout,
			String awsAccessKey, String awsSecretKey) {
		
		super();
		this.stackName = stackName;
		this.description = description;
		this.cloudFormationRecipe = cloudFormationRecipe;
		this.parameters = parameters;
		this.timeout = timeout;
		this.awsAccessKey = awsAccessKey;
		this.awsSecretKey = awsSecretKey;
		
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

	public String getAwsAccessKey() {
		return awsAccessKey;
	}

	public String getAwsSecretKey() {
		return awsSecretKey;
	}

	public Map<String, String> getParsedParameters() {
		
		if (parameters == null || parameters.isEmpty())
			return new HashMap<String, String>();
		
		Map<String, String> result = new HashMap<String, String>();
		String token[] = null;
		for (String param : parameters.split(",")) {
			token = param.split("=");
			result.put(token[0].trim(), token[1].trim());
		}
		
		return result;
	}
	
	@Extension
	public static final class DescriptorImpl extends Descriptor<StackBean>{
		
		@Override
		public String getDisplayName() {
			return "Cloud Formation";
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

	}

}