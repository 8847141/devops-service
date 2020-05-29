package io.choerodon.devops.app.task;

import static io.choerodon.devops.infra.constant.MiscConstants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import retrofit2.Call;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.ConfigVO;
import io.choerodon.devops.api.vo.sonar.UserToken;
import io.choerodon.devops.api.vo.sonar.UserTokens;
import io.choerodon.devops.app.service.DevopsConfigService;
import io.choerodon.devops.infra.dto.DevopsConfigDTO;
import io.choerodon.devops.infra.enums.ProjectConfigType;
import io.choerodon.devops.infra.feign.SonarClient;
import io.choerodon.devops.infra.handler.RetrofitHandler;
import io.choerodon.devops.infra.util.RetrofitCallExceptionParse;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  16:44 2019/3/11
 * Description:
 */
@Component
public class DevopsCommandRunner implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevopsCommandRunner.class);

    public static final String SONAR = "sonar";

    private final Gson gson = new Gson();

    @Autowired
    private DevopsConfigService devopsConfigService;
    @Value("${services.helm.url}")
    private String servicesHelmUrl;
    @Value("${services.harbor.baseUrl}")
    private String servicesHarborBaseUrl;
    @Value("${services.harbor.username}")
    private String servicesHarborUsername;
    @Value("${services.harbor.password}")
    private String servicesHarborPassword;
    @Value("${services.harbor.update:true}")
    private Boolean servicesHarborUpdate;
    @Value("${services.sonarqube.url:}")
    private String sonarqubeUrl;
    @Value("${services.sonarqube.username:}")
    private String userName;
    @Value("${services.sonarqube.password:}")
    private String password;

    @Override
    public void run(String... strings) {
        if (servicesHarborUpdate) {
            try {
                ConfigVO harborConfig = new ConfigVO();
                harborConfig.setUrl(servicesHarborBaseUrl);
                harborConfig.setUserName(servicesHarborUsername);
                harborConfig.setPassword(servicesHarborPassword);
                initConfig(harborConfig, DEFAULT_HARBOR_NAME, ProjectConfigType.HARBOR.getType());

                ConfigVO chartConfig = new ConfigVO();
                chartConfig.setUrl(servicesHelmUrl);
                initConfig(chartConfig, DEFAULT_CHART_NAME, ProjectConfigType.CHART.getType());
                if (sonarqubeUrl != null && !sonarqubeUrl.isEmpty()) {
                    createSonarToken();
                }
            } catch (Exception e) {
                throw new CommonException("error.init.project.config", e);
            }
        }
    }

    private void initConfig(ConfigVO configDTO, String configName, String configType) {
        DevopsConfigDTO newConfigDTO = new DevopsConfigDTO();
        newConfigDTO.setConfig(gson.toJson(configDTO));
        newConfigDTO.setName(configName);
        newConfigDTO.setType(configType);
        DevopsConfigDTO oldConfigDTO = devopsConfigService.baseQueryByName(null, configName);
        if (oldConfigDTO == null) {
            devopsConfigService.baseCreate(newConfigDTO);
        } else if (!gson.toJson(configDTO).equals(oldConfigDTO.getConfig())) {
            newConfigDTO.setId(oldConfigDTO.getId());
            newConfigDTO.setObjectVersionNumber(oldConfigDTO.getObjectVersionNumber());
            devopsConfigService.baseUpdate(newConfigDTO);
        }
    }

    private void createSonarToken() {
        DevopsConfigDTO oldConfigDTO = devopsConfigService.baseQueryByName(null, DEFAULT_SONAR_NAME);
        if (oldConfigDTO == null) {
            try {
                SonarClient sonarClient = RetrofitHandler.getSonarClient(sonarqubeUrl, SONAR, userName, password);
                Map<String, String> map = new HashMap<>();
                map.put("name", "ci-token");
                map.put("login", "admin");
                Call<ResponseBody> responseCall = sonarClient.listToken();
                UserTokens userTokens = RetrofitCallExceptionParse.executeCall(responseCall, "error.sonar.token.get", UserTokens.class);
                Optional<UserToken> userTokenOptional = userTokens.getUserTokens().stream().filter(userToken -> "ci-token".equals(userToken.getName())).findFirst();
                if (userTokenOptional.isPresent()) {
                    map.put("name", "ci-token-new");
                }
                Call<ResponseBody> responseCallNew = sonarClient.createToken(map);
                UserToken userToken = RetrofitCallExceptionParse.executeCall(responseCallNew, "error.create.sonar.token", UserToken.class);
                DevopsConfigDTO newConfigDTO = new DevopsConfigDTO();
                newConfigDTO.setConfig(userToken.getToken());
                newConfigDTO.setName(DEFAULT_SONAR_NAME);
                newConfigDTO.setType(ProjectConfigType.SONAR.getType());
                devopsConfigService.baseCreate(newConfigDTO);

            } catch (Exception e) {
                LOGGER.error("======创建SonarQube token失败======={}", e.getMessage());
            }
        }
    }
}
