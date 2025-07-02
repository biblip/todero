package com.social100.todero.common.config;

import lombok.Getter;

@Getter
public enum ServerType {
  AI(41),
  AIA(414);

  final Integer port;

  ServerType(Integer port) {
    this.port = port;
  }

}
