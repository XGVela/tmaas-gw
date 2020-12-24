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

package org.xgvela.cnf.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.TimeUnit;

public class ZKManager {

    private static Logger LOG = LogManager.getLogger(ZKManager.class);

    private static String endPoints = String.valueOf(System.getenv("ZK_SVC_FQDN"));

    private static CuratorFramework client = getClient();

    private static CuratorFramework getClient() throws RuntimeException {
        if (client != null) {
            return client;
        }
        LOG.info("starting the ZK client with endpoint " + endPoints);
        client = CuratorFrameworkFactory.newClient(endPoints, new ExponentialBackoffRetry(1000, 5));
        client.start();
        try {
            if (!client.blockUntilConnected(60, TimeUnit.SECONDS)) {
                throw new RuntimeException("Time out waiting to connect to ZK!");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("interrupted exception while connecting the zookeeper client", e);
        }
        LOG.info("Zk client started");
        return client;
    }

    public static void close() throws RuntimeException {
        getClient().close();
    }

    public static Stat getStat(String path) {
        try {
            return getClient().checkExists().forPath(path);
        } catch (Exception e) {
            LOG.debug("error while calling getting stat for path" + path);
        }
        return null;
    }

    public static boolean PathExist(String path) throws RuntimeException {
        if (getStat(path) != null) {
            return true;
        }
        return false;
    }

    // creates the znode
    public static void create(String path, byte[] payload) throws Exception {
        LOG.debug("ZK create method called for path : " + path);
        getClient().create().creatingParentsIfNeeded().forPath(path, payload);
    }

    public static void selectLeader(String path) throws Exception {
        Leader leader =  new Leader(getClient(),path);
        leader.start();
    }

}