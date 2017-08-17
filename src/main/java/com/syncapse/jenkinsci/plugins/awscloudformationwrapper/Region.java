package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

public enum Region {
	US_East_Northern_Virginia("US East (Northern Virginia) Region", "cloudformation.us-east-1.amazonaws.com"),
	US_WEST_Oregon("US West (Oregon) Region", "cloudformation.us-west-2.amazonaws.com"),
	US_WEST_Northern_California("US West (Northern California) Region", "cloudformation.us-west-1.amazonaws.com"),
	US_WEST_Ohio("US East (Ohio) Region", "cloudformation.us-east-2.amazonaws.com"),
	US_Govcloud_US("US GovCloud (US) Region", "cloudformation.us-gov-west-1.amazonaws.com"),
	Canada_Central("Canada (Central) Region", "cloudformation.ca-central-1.amazonaws.com"),
	EU_Ireland("EU (Ireland) Region", "cloudformation.eu-west-1.amazonaws.com"),
	EU_Frankfurt("EU (Frankfurt) Region", "cloudformation.eu-central-1.amazonaws.com"),
	EU_London("EU (London) Region", "cloudformation.eu-west-2.amazonaws.com"),
	Asia_Pacific_Singapore("Asia Pacific (Singapore) Region", "cloudformation.ap-southeast-1.amazonaws.com"),
	Asia_Pacific_Sydney("Asia Pacific (Sydney) Region", "cloudformation.ap-southeast-2.amazonaws.com"),
	Asia_Pacific_Tokyo("Asia Pacific (Tokyo) Region", "cloudformation.ap-northeast-1.amazonaws.com"),
	Asia_Pacific_Mumbai("Asia Pacific (Mumbai) Region", "cloudformation.ap-south-1.amazonaws.com"),
	Asia_Pacific_Seoul("Asia Pacific (Seoul) Region", "cloudformation.ap-northeast-2.amazonaws.com"),
	Asia_Pacific_Beijing("Asia Pacific (Beijing) Region", "cloudformation.cn-north-1.amazonaws.com.cn"),
	South_America_Sao_Paulo("South America (Sao Paulo) Region", "cloudformation.sa-east-1.amazonaws.com");

	public final String readableName;
	public final String endPoint;

	Region(String readableName, String endPoint) {
		this.readableName = readableName;
		this.endPoint = endPoint;
	}

	public static Region getDefault() {
		return US_East_Northern_Virginia;
	}

}
