package io.choerodon.devops.infra.dto;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModelProperty;

import io.choerodon.mybatis.entity.BaseDTO;

/**
 * @author: 25499
 * @date: 2019/10/28 13:56
 * @description:
 */
@Table(name = "devops_prometheus")
public class DevopsPrometheusDTO extends BaseDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ApiModelProperty("grafana.adminPassword")
    private String adminPassword;

    @ApiModelProperty("grafana.ingress.hosts")
    private String grafanaDomain;

    @ApiModelProperty("pv名称")
    private String pvName;

    @ApiModelProperty("集群id")
    private Long clusterId;

    @ApiModelProperty("replacement")
    @Transient
    private String clusterCode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getGrafanaDomain() {
        return grafanaDomain;
    }

    public void setGrafanaDomain(String grafanaDomain) {
        this.grafanaDomain = grafanaDomain;
    }

    public String getPvName() {
        return pvName;
    }

    public void setPvName(String pvName) {
        this.pvName = pvName;
    }

    public String getClusterCode() {
        return clusterCode;
    }

    public void setClusterCode(String clusterCode) {
        this.clusterCode = clusterCode;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }
}
