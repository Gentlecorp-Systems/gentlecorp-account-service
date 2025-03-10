package com.gentlecorp.account.util;

import com.gentlecorp.account.exception.VersionAheadException;
import com.gentlecorp.account.exception.VersionOutdatedException;
import com.gentlecorp.account.model.entity.Account;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VersionUtils {

  public static void validateVersion(int version, Account entity) {
    if (version < entity.getVersion()) {
      log.error("Version is outdated");
      throw new VersionOutdatedException(version);
    }
    if (version > entity.getVersion()) {
      log.error("Version is ahead of the current version");
      throw new VersionAheadException(version);
    }
  }
}
