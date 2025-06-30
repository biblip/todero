package com.social100.todero.common.config;

public enum ServerType {
  AI(41),
  AIA(414);

  final Integer port;

  ServerType(Integer port) {
    this.port = port;
  }

  public Integer getPort() {
    return port;
  }
}
