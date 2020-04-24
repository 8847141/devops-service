import React, { useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import { Spin } from 'choerodon-ui';
import map from 'lodash/map';
import isEqual from 'lodash/isEqual';
import DetailHeader from './components/detailHeader';
import DetailColumn from './components/detailColumn';
import Loading from '../../../../components/loading';

export default observer((props) => {
  const {
    gitlabPipelineId,
    detailStore,
    projectId,
    status: treeStatus,
    treeDs,
    stageRecordVOList: treeStageRecordVOList,
  } = props;

  const {
    loadDetailData,
    getDetailLoading,
    getDetailData,
  } = detailStore;

  useEffect(() => {
    loadDetailData(projectId, gitlabPipelineId);
  }, [projectId, gitlabPipelineId]);

  // stageRecordVOList: 各个详情阶段记录
  // devopsCipiplineVO: 本流水线记录得信息

  const {
    stageRecordVOList, devopsCiPipelineVO, status, gitlabPipelineId: pipelineRecordId,
  } = getDetailData;

  useEffect(() => {
    const treeStatusList = map(treeStageRecordVOList || [], 'status');
    const detailStatusList = map(stageRecordVOList || [], 'status');
    if (pipelineRecordId === gitlabPipelineId && (status !== treeStatus || !isEqual(detailStatusList, treeStatusList))) {
      treeDs && treeDs.query();
    }
  }, [pipelineRecordId]);

  const renderStage = () => (
    stageRecordVOList && stageRecordVOList.length > 0 ? stageRecordVOList.map((item) => {
      const { name, status: stageStatus, durationSeconds, sequence } = item;
      return (
        <DetailColumn
          key={sequence}
          piplineName={name}
          seconds={durationSeconds}
          piplineStatus={stageStatus}
          {...item}
          {...props}
        />
      );
    }) : '暂无数据....'
  );

  return (
    !getDetailLoading
      ? <div className="c7n-piplineManage">
        <DetailHeader
          gitlabPipelineId={gitlabPipelineId}
          parentName={devopsCiPipelineVO && devopsCiPipelineVO.name}
          status={status}
        />
        <div className="c7n-piplineManage-detail">
          {renderStage()}
        </div>
      </div> : <Loading display={getDetailLoading} />
  );
});
