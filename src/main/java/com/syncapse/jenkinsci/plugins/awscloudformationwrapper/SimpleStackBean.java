package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;

import hudson.util.Secret;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * 
 * 
 * @author erickdovale
 * 
 */
public class SimpleStackBean extends AbstractDescribableImpl<SimpleStackBean> {

	/**
	 * The name of the stack.
	 */
	private String stackName;

	/**
	 * The access key to call Amazon's APIs
	 */
	private String awsAccessKey;

	/**
	 * The secret key to call Amazon's APIs
	 */
	private Secret awsSecretKey;

	/**
	 * The AWS Region to work against.
	 */
	private Region awsRegion;
        
        private Boolean isPrefixSelected;
        
       
 
	@DataBoundConstructor
	public SimpleStackBean(String stackName, String awsAccessKey,
						   Secret awsSecretKey, Region awsRegion, Boolean isPrefixSelected) {
		this.stackName = stackName;
		this.awsAccessKey = awsAccessKey;
		this.awsSecretKey = awsSecretKey;
		this.awsRegion = awsRegion != null ? awsRegion : Region.getDefault();
                this.isPrefixSelected=isPrefixSelected;
                
          
	}

	public String getStackName() {
		return stackName;
	}

	public String getAwsAccessKey() {
		return awsAccessKey;
	}

	public Secret getAwsSecretKey() {
		return awsSecretKey;
	}
       public Boolean getIsPrefixSelected() {
		return isPrefixSelected;
	}

	public String getParsedAwsAccessKey(EnvVars env) {
		return env.expand(getAwsAccessKey());
	}

	public String getParsedAwsSecretKey(EnvVars env) {
		return env.expand(getAwsSecretKey().getPlainText());
	}

	public Region getAwsRegion() {
		return awsRegion;
	}


	@Extension
	public static final class DescriptorImpl extends
			Descriptor<SimpleStackBean> {

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