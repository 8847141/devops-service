import React, { useEffect } from 'react';
import { observer } from 'mobx-react-lite';
import { Spin } from 'choerodon-ui';
import EditColumn from '../eidtColumn';
import { usePipelineStageEditStore } from './stores';
import Loading from '../../../../../../components/loading';

const defaultData = [
  {
    name: '构建',
    sequence: 1,
    jobList: [],
    type: 'ci',
  }, {
    name: '部署',
    sequence: 2,
    jobList: [],
    type: 'cd',
  },
];

export default observer(() => {
  const {
    projectId,
    pipelineId,
    editBlockStore,
    stepStore,
    edit,
    appServiceId,
    appServiceName,
    image,
  } = usePipelineStageEditStore();
  const {
    setStepData,
    getStepData,
    getStepData2,
    loadData,
    getLoading,
  } = editBlockStore || stepStore;

  useEffect(() => {
    pipelineId && !edit ? loadData(projectId, pipelineId) : setStepData(defaultData, edit);
  }, [pipelineId, projectId]);

  function renderColumn() {
    const dataSource = edit ? getStepData2 : getStepData;
    if (dataSource && dataSource.length > 0) {
      return dataSource.map((item, index) => <EditColumn
        columnIndex={index}
        key={item.id}
        isLast={String(index) === String(dataSource.length - 1)}
        {...item}
        edit={edit}
        pipelineId={pipelineId}
        appServiceId={appServiceId}
        appServiceName={appServiceName}
        image={image}
      />);
    }
  }

  function renderBlock() {
    if (edit) {
      return (
        <div className="c7n-piplineManage-edit">
          {renderColumn()}
        </div>
      );
    } else {
      return (
        !getLoading && !edit ? <div className="c7n-piplineManage-edit">
          {renderColumn()}
        </div> : <Loading display={getLoading} />
      );
    }
  }

  return renderBlock();
});
