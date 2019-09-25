import React, { createContext, useContext, useMemo, useEffect } from 'react';
import { DataSet } from 'choerodon-ui/pro';
import { inject } from 'mobx-react';
import { observer } from 'mobx-react-lite';
import { injectIntl } from 'react-intl';
import SecretTableDataSet from './SecretTableDataSet';
import ConfigMapTableDataSet from './ConfigMapTableDataSet';
import { useResourceStore } from '../../../../stores';
import useConfigMapStore from '../../../stores/useConfigMapStore';
import useSecretStore from '../../../stores/useSecretStore';
import getTablePostData from '../../../../../../utils/getTablePostData';

const TYPE = {
  group_configMaps: 'configMap',
  group_secrets: 'cipher',
};

const Store = createContext();

export function useKeyValueStore() {
  return useContext(Store);
}


export const StoreProvider = injectIntl(inject('AppState')(
  observer((props) => {
    const { AppState: { currentMenuType: { id } }, children, contentType } = props;
    const {
      intl: { formatMessage },
      resourceStore: { getSelectedMenu: { parentId }, setUpTarget, getUpTarget },
      itemTypes: { MAP_GROUP, CIPHER_GROUP },
    } = useResourceStore();
    
    const itemType = TYPE[contentType];
   
    const ConfigMapTableDs = useMemo(() => new DataSet(ConfigMapTableDataSet({
      formatMessage,
    })), []);
    const SecretTableDs = useMemo(() => new DataSet(SecretTableDataSet({
      formatMessage,
    })), []);
    useEffect(() => {
      if (itemType === 'configMap') {
        ConfigMapTableDs.transport = {
          read: ({ data }) => {
            const postData = getTablePostData(data);
            return ({
              url: `/devops/v1/projects/${id}/config_maps/page_by_options?env_id=${parentId}`,
              method: 'post',
              data: postData,
            });
          },
          destroy: ({ data: [data] }) => ({
            url: `/devops/v1/projects/${id}/config_maps/${data.id}`,
            method: 'delete',
            data,
          }),
        };
        ConfigMapTableDs.query();
      } else {
        SecretTableDs.transport = {
          read: ({ data }) => {
            const postData = getTablePostData(data);
            return ({
              url: `/devops/v1/projects/${id}/secret/page_by_options?env_id=${parentId}`,
              method: 'post',
              data: postData,
            });
          },
          destroy: ({ data: [data] }) => ({
            url: `/devops/v1/projects/${id}/secret/${parentId}/${data.id}`,
            method: 'delete',
            data,
          }),
        };
        SecretTableDs.query();
      }
    }, [id, contentType, parentId]);

    const itemData = {
      configMap: {
        permissions: {
          edit: ['devops-service.devops-config-map.update'],
          delete: ['devops-service.devops-config-map.delete'],
        },
        formStore: useConfigMapStore(),
      },
      cipher: {
        permissions: {
          edit: ['devops-service.devops-secret.update'],
          delete: ['devops-service.devops-secret.deleteSecret'],
        },
        formStore: useSecretStore(),
      },
    };

    const value = {
      ...props,
      itemType,
      permissions: itemData[itemType].permissions,
      formStore: itemData[itemType].formStore,
      SecretTableDs,
      ConfigMapTableDs,
    };

    useEffect(() => {
      const { type, id: envId } = getUpTarget;
      if (parentId === envId) {
        if (type === MAP_GROUP) {
          ConfigMapTableDs.query();
          setUpTarget({});
        } else if (type === CIPHER_GROUP) {
          SecretTableDs.query();
          setUpTarget({});
        }
      }
    }, [getUpTarget]);

    return (
      <Store.Provider value={value}>
        {children}
      </Store.Provider>
    );
  })
));
