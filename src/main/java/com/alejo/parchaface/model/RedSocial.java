package com.alejo.parchaface.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class RedSocial {

  @Column(name = "plataforma", length = 50)
  private String platform;

  @Column(name = "handle", length = 150)
  private String handle;

  public RedSocial() {}

  public RedSocial(String platform, String handle) {
    this.platform = platform;
    this.handle = handle;
  }

  public String getPlatform() { return platform; }
  public void setPlatform(String platform) { this.platform = platform; }

  public String getHandle() { return handle; }
  public void setHandle(String handle) { this.handle = handle; }
}
