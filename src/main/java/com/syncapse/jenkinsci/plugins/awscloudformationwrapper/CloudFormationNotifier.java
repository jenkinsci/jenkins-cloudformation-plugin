package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import com.google.common.collect.ImmutableList;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.tasks.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * User: joeljohnson
 * Date: 12/14/11
 * Time: 12:45 PM
 */
public class CloudFormationNotifier extends Notifier {
	private static final Logger LOGGER = Logger.getLogger(CloudFormationNotifier.class.getName());
	private final List<SimpleStackBean> stacks;

	@DataBoundConstructor
	public CloudFormationNotifier(List<SimpleStackBean> stacks) {
		this.stacks = stacks;
	}

	public List<SimpleStackBean> getStacks() {
		return stacks;
	}


	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		LOGGER.info("prebuild");
		return super.prebuild(build, listener);
	}

       
	@Override
	public Action getProjectAction(AbstractProject<?, ?> project) {
		LOGGER.info("getProjectAction");
		return super.getProjectAction(project);
	}

	@Override
	public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
		LOGGER.info("getProjectActions");
		return super.getProjectActions(project);
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		EnvVars envVars = build.getEnvironment(listener);
		boolean result = true;
		for (SimpleStackBean stack : stacks) {
			CloudFormation cloudFormation = new CloudFormation(listener.getLogger(),
					stack.getStackName(), false, "", new HashMap<String, String>(), 0,
					stack.getParsedAwsAccessKey(envVars), stack.getParsedAwsSecretKey(envVars),
					stack.getAwsRegion(), false, envVars, stack.getIsPrefixSelected());

			if(cloudFormation.delete()) {
				LOGGER.info("Success");
			} else {
				LOGGER.warning("Failed");
				result = false;
			}
		}
		return result;
	}

	@Override
	public BuildStepDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		@Override
		public String getDisplayName() {
			return "Tear down Amazon CloudFormation";
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}
      
}

