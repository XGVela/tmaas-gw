// Copyright 2020 Mavenir
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.xgvela.cnf.commoninfraconfig;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xgvela.cnf.k8s.K8sClient;

@Component
public class CommonInfraConfig {

	@Autowired
	private K8sClient k8s;

	private static Logger LOG = LogManager.getLogger(CommonInfraConfig.class);
	@JsonProperty("logging_svc_url")
	private String fluentbitSvcUrl;

	@JsonProperty("etcd_url")
	private String etcdUrl;

	@JsonProperty("kafka_brokers")
	private String[] kafkaBrokers;

	@JsonProperty("logging_svc_tcp_port")
	private String loggingPort;

	@JsonProperty("enable_retrx")
	private String enableRetrx;

	@JsonProperty("log_format")
	private String logFormat;

	@JsonProperty("apigw_rest_fqdn")
	private String apiGwFqdn;

	@JsonProperty("tpaas_fqdn")
	private String tpaasFqdn;

	@JsonProperty("topoengine_fqdn")
	private String topoengineFqdn;


	public String getLogFormat() {
		return logFormat;
	}

	public String getTopoengineFqdn() {
		return topoengineFqdn;
	}

	public void setTopoengineFqdn(String topoengineFqdn) {
		this.topoengineFqdn = topoengineFqdn;
	}

	public void setLogFormat(String logFormat) {
		this.logFormat = logFormat;
	}

	public String getTpaasFqdn() {
		return tpaasFqdn;
	}

	public void setTpaasFqdn(String tpaasFqdn) {
		this.tpaasFqdn = tpaasFqdn;
	}

	public String getEnableRetrx() {
		return enableRetrx;
	}

	public void setEnableRetrx(String enableRetrx) {
		this.enableRetrx = enableRetrx;
	}

	public String getLoggingPort() {
		return loggingPort;
	}

	public void setLoggingPort(String loggingPort) {
		this.loggingPort = loggingPort;
	}

	private static CommonInfraConfig single_instance = null;

	private void getCommonInfraConfig() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			single_instance = mapper.readValue(new FileInputStream("/opt/conf/static/common-infra.json"),
					CommonInfraConfig.class);
		} catch (IOException e) {
			LOG.error("Error occured while parsing common-infra file: " + e.getMessage());
		}
		single_instance.setApiGwFqdn(getFqdn("config-service"));
		single_instance.setTpaasFqdn(getFqdn("tpaas"));
		single_instance.setTopoengineFqdn(getFqdn("topo-engine"));

	}

	public String getApiGwFqdn() {
		return apiGwFqdn;
	}

	public void setApiGwFqdn(String apiGwFqdn) {
		this.apiGwFqdn = apiGwFqdn;
	}

	private String getFqdn(String microservice) {
		String fqdn = "";
		switch (microservice) {
		case "config-service":
			fqdn = k8s.getServiceName(microservice) + "." + System.getenv("K8S_NAMESPACE")
					+ ".svc.cluster.local:8008";
			break;
		case "tpaas":
			fqdn = k8s.getServiceName(microservice) + "." + System.getenv("K8S_NAMESPACE")
					+ ".svc.cluster.local:7070";
			break;
		case "topo-engine":
			fqdn = k8s.getServiceName(microservice) + "." + System.getenv("K8S_NAMESPACE")
					+ ".svc.cluster.local:8080";
			break;


		}
		LOG.debug(microservice + " FQDN = " + fqdn);
		return fqdn;
	}

	public CommonInfraConfig instance() {
		if (single_instance == null)
			getCommonInfraConfig();

		return single_instance;
	}

	public String getFluentbitSvcUrl() {
		return fluentbitSvcUrl;
	}

	public String getEtcdUrl() {
		return etcdUrl;
	}

	public String[] getKafkaBrokers() {
		return kafkaBrokers;
	}

	public void setFluentbitSvcUrl(String fluentbitSvcUrl) {
		this.fluentbitSvcUrl = fluentbitSvcUrl;
	}

	public void setEtcdUrl(String etcdUrl) {
		this.etcdUrl = etcdUrl;
	}

	public void setKafkaBrokers(String[] kafkaBrokers) {
		this.kafkaBrokers = kafkaBrokers;
	}

}
