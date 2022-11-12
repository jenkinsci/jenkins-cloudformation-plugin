package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

public enum Capability {
    Capability_IAM("no or default named IAM resources","CAPABILITY_IAM"),
    Capability_Named_IAM("custom named IAM resources","CAPABILITY_NAMED_IAM");

    public final String readableName;
    public final String name;

    Capability(String readableName, String name){
        this.readableName = readableName;
        this.name = name;
    }

    public static Capability getDefault() {
        return Capability_IAM;
    }

    }
