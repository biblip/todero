# 2025/06/30
## todero
- Main project

## common
- Common modules

## transport-protocol
- AIA protocol

## tcp-server 
- No used

## aia-server 
-Aia server implementation

## processor 
- Annotation processor
- 
## annotations
- Annotations

## Plugins
- vlc-plugin
- email-plugin 
- simple-plugin 
- aia-protocol-plugin 
- ssh-plugin 
- agent-demo

## aia-web 
- in progress
## ssh-server
- in progress
## server 
- uses aia-server and connects using aia-protocol
## console
- connects to server via aia-protocol

## Usage
- build ```mvn clean install``` 
- dir ```playground``` is created during build.

```cd playground```

```java -jar aia-server.jar``` 
- UDP listens on two ports 41 and 414

```java -jar console.jar [--aia]```
- connects to UDP port 41 or 414 based on flag --aia
- once console is ready, type ```help```

## Ports
- Port 41 agents are accessible.
- Port 414 tools are accessible.