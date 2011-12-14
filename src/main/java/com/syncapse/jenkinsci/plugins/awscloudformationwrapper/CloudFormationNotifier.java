package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
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
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * User: joeljohnson
 * Date: 12/14/11
 * Time: 12:45 PM
 */
public class CloudFormationNotifier extends Notifier {
	private static final Logger LOGGER = Logger.getLogger(CloudFormationNotifier.class.getName());
	private final String stackName;
	private final String accessKey;
	private final String secretKey;

	@DataBoundConstructor
	public CloudFormationNotifier(String stackName, String accessKey, String secretKey) {
		this.stackName = stackName;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
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
		CloudFormation cloudFormation = new CloudFormation(new PrintStream(new ByteOutputStream()), stackName, "", new HashMap<String, String>(), 0, accessKey, secretKey, false, envVars);
		if(cloudFormation.delete()) {
			LOGGER.info("Success");
			return true;
		} else {
			LOGGER.warning("Failed");
			return false;
		}

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

