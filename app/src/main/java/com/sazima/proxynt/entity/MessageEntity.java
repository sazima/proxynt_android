package com.sazima.proxynt.entity;


public class MessageEntity<T> {
    private String type_;
    private T data;

    public String getType_() {
        return type_;
    }

    public void setType_(String type_) {
        this.type_ = type_;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "MessageEntity{" +
                "type_='" + type_ + '\'' +
                ", data=" + data +
                '}';
    }
}
