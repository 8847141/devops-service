import React, { createContext, useContext, useEffect, useMemo } from 'react';
import { inject } from 'mobx-react';
import { observer } from 'mobx-react-lite';
import { injectIntl } from 'react-intl';
import { DataSet } from 'choerodon-ui/pro';
import FormDataSet from './FormDataSet';
import GroupOptionDataSet from '../../env-create/stores/GroupOptionDataSet';

const Store = createContext();

export function useFormStore() {
  return useContext(Store);
}

export const StoreProvider = injectIntl(inject('AppState')(
  observer((props) => {
    const {
      AppState: { currentMenuType: { id: projectId } },
      intl: { formatMessage },
      children,
      intlPrefix,
      record,
    } = props;
    const formDs = useMemo(() => new DataSet(FormDataSet({ formatMessage, intlPrefix, projectId })), [projectId]);
    const groupOptionDs = useMemo(() => new DataSet(GroupOptionDataSet(projectId)), [projectId]);

    useEffect(() => {
      if (record) {
        const envId = record.get('id');
        formDs.transport.read.url = `/devops/v1/projects/${projectId}/envs/${envId}`;
        formDs.query();
      }
    }, [record]);

    const value = {
      ...props,
      formDs,
      intlPrefix,
      groupOptionDs,
    };
    return (
      <Store.Provider value={value}>
        {children}
      </Store.Provider>
    );
  })
));
