package com.social100.todero.plugin.ssh.service;

public record CommandResult(int exitCode, String stdout, String stderr) {}

