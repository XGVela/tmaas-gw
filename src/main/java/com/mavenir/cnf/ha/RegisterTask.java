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

package org.xgvela.cnf.ha;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.xgvela.cnf.ha.model.RegisterRequest;
import org.xgvela.cnf.k8s.PodWatcher;
import org.xgvela.logging.NatsAppender;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

public class RegisterTask {

	private static HttpEntity<String> entity;
	private static RestTemplate restTemplate = new RestTemplate();

	public static void init() {
		System.out.println("Register Task init method invoked");
		setRegisterRequestEntity();
		try {
			register();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// try XGVela registration indefinitely
	public static void register() throws InterruptedException {
		final String registerUrl = "http://localhost:6060/api/v1/_operations/xgvela/register";
		while (true) {
			System.out.println("Registration task running at: " + new Date());
			try {
				ResponseEntity<String> response = restTemplate.exchange(registerUrl, HttpMethod.POST, entity,
						String.class);
				System.out.println("Response: " + response.getBody() + ", StatusCode: " + response.getStatusCode()
						+ ", Value: " + response.getStatusCodeValue());

				// xgvela registration successful
				if (response.getStatusCode() == HttpStatus.OK) {
					System.out.println("Registration successful; status success returned from CIM");
					break;
				}

			} catch (HttpClientErrorException e) {
				System.out.println(e.getRawStatusCode());
				System.out.println(e.getResponseBodyAsString());

			} catch (Exception e) {
				e.printStackTrace();

			} finally {
				Thread.sleep(1000);
			}
		}
	}

	private static void setRegisterRequestEntity() {
		try {
			entity = new HttpEntity<String>(PodWatcher.mapper
					.writeValueAsString(new RegisterRequest(NatsAppender.TopoGateway, NatsAppender.K8sContainer)));
		} catch (JsonProcessingException e) {
			System.out.println(e.getMessage());
		}
	}
}
