package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

 import hudson.EnvVars;
 import hudson.model.EnvironmentContributingAction;
 import hudson.model.AbstractBuild;
 
 /**
  * Allow plugin to update environment variables
  *
  * @author Andrew Sumner
  *
  */
 public class VariableInjectionAction implements EnvironmentContributingAction {
 
     private String key;
     private String value;
 
     public VariableInjectionAction(String key, String value) {
         this.key = key;
         this.value = value;
     }
 
     public void buildEnvVars(AbstractBuild build, EnvVars envVars) {
 
         if (envVars != null && key != null && value != null) {
             envVars.put(key, value);
         }
     }
 
     public String getDisplayName() {
         return "VariableInjectionAction";
     }
 
     public String getIconFileName() {
         return null;
     }
 
     public String getUrlName() {
         return null;
     }
 } 