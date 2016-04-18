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
	private String awsSecretKey;

	/**
	 * The AWS Region to work against.
	 */
	private Region awsRegion;
        
	private Boolean isPrefixSelected;

	/**
	 * number of stack to retain on tear down
	 */
	private long retainStacksQty = 1;
       
 
	@DataBoundConstructor
	public SimpleStackBean(String stackName, String awsAccessKey,
			String awsSecretKey, Region awsRegion,Boolean isPrefixSelected, long retainStacksQty) {
		this.stackName = stackName;
		this.awsAccessKey = awsAccessKey;
		this.awsSecretKey = awsSecretKey;
		this.awsRegion = awsRegion != null ? awsRegion : Region.getDefault();
		this.isPrefixSelected=isPrefixSelected;
		this.retainStacksQty = retainStacksQty;
	}

	public String getStackName() {
		return stackName;
	}

	public String getAwsAccessKey() {
		return awsAccessKey;
	}

	public String getAwsSecretKey() {
		return awsSecretKey;
	}

    public Boolean getIsPrefixSelected() {
		return isPrefixSelected;
	}

    public long getRetainStacksQty() {
        return retainStacksQty;
    }

	public String getParsedAwsAccessKey(EnvVars env) {
		return env.expand(getAwsAccessKey());
	}

	public String getParsedAwsSecretKey(EnvVars env) {
		return env.expand(getAwsSecretKey());
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

        public FormValidation doCheckretainStacksQty(
                @AncestorInPath AbstractProject<?, ?> project,
                @QueryParameter String value) throws IOException {
            if (value.length() > 0) {
                try {
                    Long.parseLong(value);
                } catch (NumberFormatException e) {
                    return FormValidation.error("Retain Stack Qty value "+ value + " is not a number.");
                }
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