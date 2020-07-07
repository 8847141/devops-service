package io.choerodon.devops.api.vo;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author scp
 * @date 2020/6/30
 * @description 主机部署配置信息
 */
public class CdHostDeployConfigVO {
    @ApiModelProperty("主机Ip")
    private String hostIp;

    @ApiModelProperty("主机port")
    private String hostPort;

    @ApiModelProperty("用户名")
    private String userName;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("账号配置类型")
    private String accountType;

    @ApiModelProperty("主机部署类型 image/jar/customize")
    // HostDeployType
    private String hostDeployType;

    @ApiModelProperty("镜像部署详情")
    private ImageDeploy imageDeploy;

    @ApiModelProperty("jar部署详情")
    private JarDeploy jarDeploy;

    @ApiModelProperty("自定义部署customize详情")
    private Customize customize;

    public class ImageDeploy {
        @ApiModelProperty("仓库名")
        private String repoName;

        @ApiModelProperty("仓库类型")
        private String repoType;

        @ApiModelProperty("仓库Id")
        private String repoId;

        @ApiModelProperty("镜像名称")
        private String imageName;

        @ApiModelProperty("匹配类型")
        private String matchType;

        @ApiModelProperty("匹配内容")
        private String matchContent;

        @ApiModelProperty("部署values")
        private String values;

        public String getValues() {
            return values;
        }

        public void setValues(String values) {
            this.values = values;
        }

        public String getRepoType() {
            return repoType;
        }

        public void setRepoType(String repoType) {
            this.repoType = repoType;
        }

        public String getRepoId() {
            return repoId;
        }

        public void setRepoId(String repoId) {
            this.repoId = repoId;
        }

        public String getRepoName() {
            return repoName;
        }

        public void setRepoName(String repoName) {
            this.repoName = repoName;
        }

        public String getImageName() {
            return imageName;
        }

        public void setImageName(String imageName) {
            this.imageName = imageName;
        }

        public String getMatchType() {
            return matchType;
        }

        public void setMatchType(String matchType) {
            this.matchType = matchType;
        }

        public String getMatchContent() {
            return matchContent;
        }

        public void setMatchContent(String matchContent) {
            this.matchContent = matchContent;
        }
    }

    public class JarDeploy {

        @ApiModelProperty("服务名")
        private String serverName;
        @ApiModelProperty("仓库名")
        private String neRepositoryName;

        @ApiModelProperty("仓库id")
        private Long repositoryId;

        @ApiModelProperty("groupId")
        private String groupId;

        @ApiModelProperty("artifactId")
        private String artifactId;

        @ApiModelProperty("版本正则")
        private String versionRegular;

        @ApiModelProperty("部署values")
        private String values;

        public String getValues() {
            return values;
        }

        public void setValues(String values) {
            this.values = values;
        }

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public String getNeRepositoryName() {
            return neRepositoryName;
        }

        public void setNeRepositoryName(String neRepositoryName) {
            this.neRepositoryName = neRepositoryName;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersionRegular() {
            return versionRegular;
        }

        public void setVersionRegular(String versionRegular) {
            this.versionRegular = versionRegular;
        }

        public Long getRepositoryId() {
            return repositoryId;
        }

        public void setRepositoryId(Long repositoryId) {
            this.repositoryId = repositoryId;
        }
    }

    public class Customize {

        @ApiModelProperty("部署values")
        private String values;

        public String getValues() {
            return values;
        }

        public void setValues(String values) {
            this.values = values;
        }
    }

    public String getHostDeployType() {
        return hostDeployType;
    }

    public void setHostDeployType(String hostDeployType) {
        this.hostDeployType = hostDeployType;
    }

    public Customize getCustomize() {
        return customize;
    }

    public void setCustomize(Customize customize) {
        this.customize = customize;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getHostPort() {
        return hostPort;
    }

    public void setHostPort(String hostPort) {
        this.hostPort = hostPort;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public ImageDeploy getImageDeploy() {
        return imageDeploy;
    }

    public void setImageDeploy(ImageDeploy imageDeploy) {
        this.imageDeploy = imageDeploy;
    }

    public JarDeploy getJarDeploy() {
        return jarDeploy;
    }

    public void setJarDeploy(JarDeploy jarDeploy) {
        this.jarDeploy = jarDeploy;
    }
}
