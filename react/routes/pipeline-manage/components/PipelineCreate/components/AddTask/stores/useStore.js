import { useLocalStore } from 'mobx-react-lite';
import { axios, Choerodon } from '@choerodon/boot';

export default function useStore() {
  return useLocalStore(() => ({
    axiosConnectTest(data, projectId) {
      return axios.post(`/devops/v1/projects/${projectId}/ci_jobs/sonar/connect`, data);
    },
  }));
}
