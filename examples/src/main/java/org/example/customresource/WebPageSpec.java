package org.example.customresource;

public class WebPageSpec {
  private String igniteNodeCpu = "1";
  private String igniteNodeMemory = "3Gi";  // request and limit share the same memory size for now
  public String getIgniteNodeCpu() {
    return igniteNodeCpu;
  }

  public String getIgniteNodeMemory() {
    return igniteNodeMemory;
  }

  private String html;
  private Boolean exposed = false;

  public String getHtml() {
    return html;
  }

  public void setHtml(String html) {
    this.html = html;
  }

  public Boolean getExposed() {
    return exposed;
  }

  public WebPageSpec setExposed(Boolean exposed) {
    this.exposed = exposed;
    return this;
  }

  @Override
  public String toString() {
    return "WebPageSpec{" +
        "html='" + html + '\'' +
        '}';
  }
}
