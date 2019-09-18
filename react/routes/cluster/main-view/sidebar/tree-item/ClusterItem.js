import React, { Fragment, useMemo } from 'react';
import PropTypes from 'prop-types';
import { injectIntl } from 'react-intl';
import { Action } from '@choerodon/master';
import { Modal } from 'choerodon-ui/pro';
import { useClusterStore } from '../../../stores';
import { useClusterMainStore } from '../../stores';
import StatusDot from '../../../../../components/status-dot';
import ActivateCluster from '../../contents/cluster-content/modals/activate-cluster';
import { useTreeStore } from './stores';
import { handlePromptError } from '../../../../../utils';
import EditCluster from '../../contents/cluster-content/modals/create-cluster';
import CustomConfirm from '../../../../../components/custom-confirm';

const ActivateClusterModalKey = Modal.key();
const EditClusterModalKey = Modal.key();
function ClusterItem({
  record,
  name,
  intlPrefix,
  intl: { formatMessage },
}) {
  const { treeDs } = useClusterStore();
  const { mainStore, ClusterDetailDs } = useClusterMainStore();

  const { projectId, treeItemStore } = useTreeStore();

  const customConfirm = useMemo(() => new CustomConfirm({ formatMessage }), []);

  function getStatus() {
    const connect = record.get('connect');
    const upgrade = record.get('upgrade');
    if (upgrade) {
      return ['disconnect'];
    } else if (connect) {
      return ['running', 'connect'];
    }
    return ['disconnect'];
  }

  
  function freshMenu() {
    treeDs.query();
  }

  function deleteItem() {
    customConfirm.delete({
      titleId: `${intlPrefix}.action.delete.title`,
      titleVal: {
        name: record.data.name,
      },
      contentId: `${intlPrefix}.action.delete.msg`,
      handleOk: () => {
        mainStore.deleteCluster({ projectId, clusterId: record.data.id })
          .then((res) => {
            if (handlePromptError(res, false)) {
              freshMenu();
            }
          });
      },
    });
  }

  function openEdit(res) {
    Modal.open({
      key: EditClusterModalKey,
      title: formatMessage({ id: `${intlPrefix}.modal.edit` }),
      children: <EditCluster isEdit record={res || ClusterDetailDs.current} mainStore={mainStore} afterOk={freshMenu} intlPrefix={intlPrefix} formatMessage={formatMessage} treeItemStore={treeItemStore} projectId={projectId} />,
      drawer: true,
      style: {
        width: 380,
      },
      okText: formatMessage({ id: 'save' }),
    });
  }

  async function editItem() {
    if (record.data.id !== ClusterDetailDs.current.get('id')) {
      const res = await treeItemStore.queryClusterDetail(projectId, record.data.id);
      openEdit(res);
      return;
    }
    openEdit();
  }

  async function activateItem() {
    const res = await treeItemStore.queryActivateClusterShell(projectId, record.get('id'));
    if (handlePromptError(res)) {
      Modal.open({
        key: ActivateClusterModalKey,
        title: formatMessage({ id: `${intlPrefix}.activate.header` }),
        children: <ActivateCluster cmd={res} intlPrefix={intlPrefix} formatMessage={formatMessage} />,
        drawer: true,
        style: {
          width: 380,
        },
        okCancel: false,
        okText: formatMessage({ id: 'close' }),
      });
    }
  }

  const getPrefix = useMemo(() => <StatusDot
    size="small"
    getStatus={getStatus}
  />, [record]);

  const getSuffix = useMemo(() => {
    const [status] = getStatus();
    const Data = [{
      service: ['devops-service.devops-cluster.update'],
      text: formatMessage({ id: `${intlPrefix}.action.edit` }),
      action: editItem,
    }];
    if (status === 'disconnect') {
      Data.push({
        service: ['devops-service.devops-cluster.queryShell'],
        text: formatMessage({ id: `${intlPrefix}.activate.header` }),
        action: activateItem,
      }, {
        service: ['devops-service.devops-cluster.deleteCluster'],
        text: formatMessage({ id: `${intlPrefix}.action.delete` }),
        action: deleteItem,
      });
    }
    return <Action placement="bottomRight" data={Data} />;
  }, []);
  
  const clearClick = (e) => {
    e.stopPropagation();
  };
  return <Fragment>
    {getPrefix}
    {name}
    <div onClick={clearClick}>
      {getSuffix}
    </div>
  </Fragment>;
}

ClusterItem.propTypes = {
  name: PropTypes.any,
};

export default injectIntl(ClusterItem);
