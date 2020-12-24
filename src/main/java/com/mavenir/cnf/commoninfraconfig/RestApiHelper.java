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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@Service
public class RestApiHelper {

	@Autowired
	private CommonInfraConfig commonInfraConfig;
	private static Logger LOG = LogManager.getLogger(RestApiHelper.class);

	public String getStaticConfig() {

		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		try {
			String response = ow.writeValueAsString(commonInfraConfig.instance());
			LOG.debug("common-infra: " + response);
			return response;
		} catch (JsonProcessingException e) {
			LOG.error("Error occured while parsing common-infra config " + e);
		}
		return "could not process request,try again!";
	}

}
