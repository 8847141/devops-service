package io.choerodon.devops.infra.convertor;

<<<<<<< HEAD

import io.choerodon.devops.infra.dto.DevopsEnvPodDTO;
import org.springframework.stereotype.Component;
=======
import io.choerodon.core.convertor.ConvertorI;
<<<<<<< HEAD
import io.choerodon.devops.api.vo.DevopsEnvPodVO;
=======
>>>>>>> [IMP]重构后端代码
import io.choerodon.devops.api.vo.iam.entity.DevopsEnvPodE;
<<<<<<< HEAD
import org.springframework.beans.BeanUtils;

<<<<<<< HEAD:src/main/java/io/choerodon/devops/domain/application/convertor/DevopsEnvPodConvertor.java
import io.choerodon.devops.infra.dataobject.DevopsEnvPodDO;
=======
>>>>>>> f7b3373a9ccceea0bbd4235a0e8f042f20369f6a:src/main/java/io/choerodon/devops/infra/convertor/DevopsEnvPodConvertor.java
>>>>>>> [IMP] 重构Repository
=======
>>>>>>> [IMP]修改后端代码结构

/**
 * Created by Zenger on 2018/4/2.
 */
@Component
<<<<<<< HEAD
public class DevopsEnvPodConvertor implements ConvertorI<DevopsEnvPodE, DevopsEnvPodDTO, DevopsEnvironmentPodVO> {

    @Override
    public DevopsEnvironmentPodVO entityToDto(DevopsEnvPodE entity) {
        DevopsEnvironmentPodVO devopsEnvironmentPodVO = new DevopsEnvironmentPodVO();
        BeanUtils.copyProperties(entity, devopsEnvironmentPodVO);
        return devopsEnvironmentPodVO;
    }

    @Override
    public DevopsEnvPodE doToEntity(DevopsEnvPodDTO dataObject) {
=======
public class DevopsEnvPodConvertor implements ConvertorI<DevopsEnvPodE, DevopsEnvPodDTO, DevopsEnvPodVO> {

    @Override
    public DevopsEnvPodVO entityToDto(DevopsEnvPodE entity) {
        DevopsEnvPodVO devopsEnvPodDTO = new DevopsEnvPodVO();
        BeanUtils.copyProperties(entity, devopsEnvPodDTO);
        return devopsEnvPodDTO;
    }

    @Override
    public DevopsEnvPodE doToEntity(DevopsEnvPodDTO dataObject) {
>>>>>>> [IMP] 重构Repository
        DevopsEnvPodE devopsEnvPodE = new DevopsEnvPodE();
        BeanUtils.copyProperties(dataObject, devopsEnvPodE);
        devopsEnvPodE.initApplicationInstanceE(dataObject.getAppInstanceId());
        return devopsEnvPodE;
    }

    @Override
<<<<<<< HEAD
    public DevopsEnvPodDTO entityToDo(DevopsEnvPodE entity) {
        DevopsEnvPodDTO devopsEnvironmentPodDTO = new DevopsEnvPodDTO();
=======
    public DevopsEnvPodDTO entityToDo(DevopsEnvPodE entity) {
        DevopsEnvPodDTO devopsEnvPodDO = new DevopsEnvPodDTO();
>>>>>>> [IMP] 重构Repository
        if (entity.getApplicationInstanceE() != null) {
            devopsEnvironmentPodDTO.setAppInstanceId(entity.getApplicationInstanceE().getId());
        }
        BeanUtils.copyProperties(entity, devopsEnvironmentPodDTO);
        return devopsEnvironmentPodDTO;
    }


    @Override
<<<<<<< HEAD
    public DevopsEnvironmentPodVO doToDto(DevopsEnvPodDTO devopsEnvironmentPodDTO) {
        DevopsEnvironmentPodVO devopsEnvironmentPodVO = new DevopsEnvironmentPodVO();
        BeanUtils.copyProperties(devopsEnvironmentPodDTO, devopsEnvironmentPodVO);
        devopsEnvironmentPodVO.setCreationDate(devopsEnvironmentPodDTO.getCreationDate());
        return devopsEnvironmentPodVO;
=======
    public DevopsEnvPodVO doToDto(DevopsEnvPodDTO devopsEnvPodDO) {
        DevopsEnvPodVO devopsEnvPodDTO = new DevopsEnvPodVO();
        BeanUtils.copyProperties(devopsEnvPodDO, devopsEnvPodDTO);
        devopsEnvPodDTO.setCreationDate(devopsEnvPodDO.getCreationDate());
        return devopsEnvPodDTO;
>>>>>>> [IMP] 重构Repository
    }
}
