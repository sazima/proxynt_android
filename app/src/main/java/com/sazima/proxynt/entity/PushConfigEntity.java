package com.sazima.proxynt.entity;

import java.util.List;

import lombok.Data;

public class PushConfigEntity {
    public  static class ClientData{
        private String name;
        private Integer remote_port;
        private Integer local_port;
        private String local_ip;
        private float speed_limit;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getRemote_port() {
            return remote_port;
        }

        public void setRemote_port(Integer remote_port) {
            this.remote_port = remote_port;
        }

        public Integer getLocal_port() {
            return local_port;
        }

        public void setLocal_port(Integer local_port) {
            this.local_port = local_port;
        }

        public String getLocal_ip() {
            return local_ip;
        }

        public void setLocal_ip(String local_ip) {
            this.local_ip = local_ip;
        }

        public float getSpeed_limit() {
            return speed_limit;
        }

        public void setSpeed_limit(float speed_limit) {
            this.speed_limit = speed_limit;
        }
    }

    private String key;
    private String version;
    private List<ClientData> config_list;
    private String client_name;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<ClientData> getConfig_list() {
        return config_list;
    }

    public void setConfig_list(List<ClientData> config_list) {
        this.config_list = config_list;
    }

    public String getClient_name() {
        return client_name;
    }

    public void setClient_name(String client_name) {
        this.client_name = client_name;
    }

    @Override
    public String toString() {
        return "PushConfigEntity{" +
                "key='" + key + '\'' +
                ", version='" + version + '\'' +
                ", config_list=" + config_list +
                ", client_name='" + client_name + '\'' +
                '}';
    }
}
