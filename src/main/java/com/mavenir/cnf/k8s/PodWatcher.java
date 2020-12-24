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

package org.xgvela.cnf.k8s;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.kafka.PodDetails;
import org.xgvela.cnf.zk.Leader;
import org.xgvela.cnf.zk.ZKManager;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

@Component
public class PodWatcher {

	private static final Logger LOG = LogManager.getLogger(PodWatcher.class);
	public static final String X_CORRELATION_ID = "X-CorrelationId";
	private static String selfXGVelaId = "xgvela1";

	public static String podName = String.valueOf(System.getenv("K8S_POD_ID"));
	public static String podNamespace = String.valueOf(System.getenv("K8S_NAMESPACE"));
	public static ObjectMapper mapper = new ObjectMapper();

	public static final CountDownLatch registrationLatch = new CountDownLatch(1);
	public static final CountDownLatch waitLatch = new CountDownLatch(1);
	public static final String TOPO_GW_LEADER_ELECTION = "/topo_gw/election";
	public static final String TOPO_GW_START = "/topo_gw/start";


	@Autowired
	private K8sClient k8sClient;

	@Autowired
	private KafkaTemplate<String, PodDetails> kafkaTemplate;

	private DeferredResult<Boolean> pushToKafka(PodDetails podDetails, String messageKey) {
		String correlationId = UUID.randomUUID().toString();
		LOG.debug("Message being pushed is: " + podDetails.toString() + ", Key: " + messageKey + ", CorrelationId: "+ correlationId);
//		final Message<PodDetails> message = MessageBuilder.withPayload(podDetails)
//				.setHeader(KafkaHeaders.TOPIC, Constants.KAFKA_TOPIC).setHeader(X_CORRELATION_ID, correlationId)
//				.build();

		List<Header> headers = new ArrayList<>();
		headers.add(new RecordHeader(X_CORRELATION_ID, correlationId.getBytes()));
		ProducerRecord<String, PodDetails> message = new ProducerRecord<String,PodDetails>(Constants.KAFKA_TOPIC, null, messageKey, podDetails, headers);

		DeferredResult<Boolean> flag = new DeferredResult<>();
		ListenableFuture<SendResult<String, PodDetails>> future = kafkaTemplate.send(message);

		future.addCallback(new ListenableFutureCallback<SendResult<String, PodDetails>>() {

			public void onSuccess(SendResult<String, PodDetails> result) {
				flag.setResult(true);
			}

			public void onFailure(Throwable ex) {
				LOG.error("Unable to send message due to : " + ex.getMessage());
				flag.setResult(false);
			}
		});
		return flag;
	}

	@PostConstruct
	public void startWatchingPod() throws Exception {
		ZKManager.selectLeader(TOPO_GW_LEADER_ELECTION);
		// await registration and NATS connection
		registrationLatch.await();
		new Thread(() -> {
			try {
				startPodWatch();
			} catch (InterruptedException e) {
				LOG.error(e.getMessage(),e);
			}
		}).start();

	}

	public void startPodWatch() throws InterruptedException {

		setXGVelaId();
		//Leader should first add the path so as to start listening on pod events
		//Reason to use this check is leader election is asynchronous and we might loose
		//few events if leader is selected a bit later but we start processing event
		while (!ZKManager.PathExist(TOPO_GW_START)){
			Thread.sleep(200);
		}
		LOG.info("Connecting to Pod Watch... ");
		k8sClient.getClient().pods().inAnyNamespace().watch((new Watcher<Pod>() {

			@Override
			public void eventReceived(Action action, Pod pod) {

				String podName = pod.getMetadata().getName();
				String namespace = pod.getMetadata().getNamespace();
				LOG.debug("Action: " + action.toString() + ", Pod: " + podName + ", Namespace: " + namespace);

				try {
					if (pod.getMetadata().getAnnotations() != null
							&& pod.getMetadata().getAnnotations().containsKey(Constants.ANNOTATION_TMAAS)) {

						JsonNode annotations = mapper
								.readTree(pod.getMetadata().getAnnotations().get(Constants.ANNOTATION_TMAAS));

						if (isValid(annotations)) {

							String xgvelaId = annotations.get(Constants.XGVELA_ID).asText();
							if (!xgvelaId.equals(selfXGVelaId)) {
								LOG.debug("Pod xgvelaId does not match: " + xgvelaId);
								return;
							}

							String nfName = annotations.get(Constants.NF_ID).asText();
							String nfType = annotations.get(Constants.NF_TYPE).asText();
							String nfServiceName = annotations.get(Constants.NF_SERVICE_ID).asText();
							String nfServiceType = annotations.get(Constants.NF_SERVICE_TYPE).asText();
							if (!Leader.leaderFlag.get()) {
								//For topo-gw dont return let it push to kafka even if its non-leader
								if (!nfServiceName.equalsIgnoreCase("topo-gw")) {
									return;
								}
							}
							pushToKafka(
									new PodDetails(action, podName, namespace, nfName, nfType, nfServiceName,
											nfServiceType),nfName);

						} else {
							LOG.debug("Pod named: [" + podName
									+ "] has TMaaS annotation but does not have all its required values");
						}
					} else {
						LOG.debug("Pod named: [" + podName
								+ "] does not have required annotations/microservice label for TMaaS");
					}
				} catch (Exception e) {
					/*
					 * any exception inside method cause watcher onClose method to be invoked with
					 * 'too old resource version' error', so this is a safety catch-all clause
					 */
					LOG.error(e.getMessage(), e);
				}
			}

			@Override
			public void onClose(KubernetesClientException cause) {
				if (cause != null)
					LOG.error(cause.getMessage() + ", " + cause.getCode() + ", " + cause.getStatus().getMetadata()
							+ ", \n" + cause);
				LOG.error("Exiting application");
				System.exit(1);
			}
		}));
	}

	private boolean isValid(JsonNode annotations) {
		return (annotations.has(Constants.NF_ID) && annotations.has(Constants.NF_SERVICE_ID)
				&& annotations.has(Constants.NF_TYPE) && annotations.has(Constants.NF_SERVICE_TYPE)
				&& annotations.has(Constants.XGVELA_ID));
	}

	private void setXGVelaId() {
		if (podName.equals("null") || podNamespace.equals("null")) {
			LOG.debug("Setting default self-xgvelaId: [xgvela1]");
			return;
		}

		try {
			selfXGVelaId = mapper
					.readTree(k8sClient.getClient().pods().inNamespace(podNamespace).withName(podName).get()
							.getMetadata().getAnnotations().get(Constants.ANNOTATION_TMAAS))
					.get(Constants.XGVELA_ID).asText();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
		LOG.debug("Self-xgvelaId: [" + selfXGVelaId + "]");
	}
}