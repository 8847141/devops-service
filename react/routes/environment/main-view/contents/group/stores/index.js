import React, { createContext, useContext, useMemo, useEffect } from 'react';
import { inject } from 'mobx-react';
import { observer } from 'mobx-react-lite';
import { injectIntl } from 'react-intl';
import { DataSet } from 'choerodon-ui/pro';
import { useEnvironmentStore } from '../../../../stores';
import TableDataSet from './TableDataSet';

const Store = createContext();

export function useEnvGroupStore() {
  return useContext(Store);
}

export const StoreProvider = injectIntl(inject('AppState')(
  observer((props) => {
    const { intl: { formatMessage }, children, AppState: { currentMenuType: { id: projectId } } } = props;
    const {
      intlPrefix,
      envStore: {
        getSelectedMenu: { id },
      },
    } = useEnvironmentStore();
    const groupDs = useMemo(() => new DataSet(TableDataSet({ formatMessage, intlPrefix })), []);

    useEffect(() => {
      const param = typeof id === 'number' && id ? `?group_id=${id}` : '';
      groupDs.transport.read.url = `/devops/v1/projects/${projectId}/envs/list_by_group${param}`;
      groupDs.query();
    }, [id, projectId]);

    const value = {
      ...props,
      groupDs,
    };
    return (
      <Store.Provider value={value}>
        {children}
      </Store.Provider>
    );
  })
));
