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

package org.xgvela.cnf.ha.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class RegisterResponse {

	@JsonProperty("status")
	private RegisterResponse.Status status;

	public RegisterResponse() {
	}

	public RegisterResponse(Status status) {
		super();
		this.status = status;
	}

	public RegisterResponse.Status getStatus() {
		return status;
	}

	public void setStatus(RegisterResponse.Status status) {
		this.status = status;
	}

	public enum Status {

		SUCCESS("success"), FAILURE("failure");
		private final String value;

		private final static Map<String, RegisterResponse.Status> CONSTANTS = new HashMap<String, RegisterResponse.Status>();

		static {
			for (RegisterResponse.Status c : values()) {
				CONSTANTS.put(c.value, c);
			}
		}

		private Status(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

		@JsonValue
		public String value() {
			return this.value;
		}

		@JsonCreator
		public static RegisterResponse.Status fromValue(String value) {
			RegisterResponse.Status constant = CONSTANTS.get(value);
			if (constant == null) {
				throw new IllegalArgumentException(value);
			} else {
				return constant;
			}
		}

	}
}
