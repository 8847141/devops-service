import React, { Component } from 'react';
import { withRouter } from 'react-router-dom';
import { observer } from 'mobx-react';
import PropTypes from 'prop-types';
import { FormattedMessage } from 'react-intl';
import { Button } from 'choerodon-ui';
import './NoChart.less';
import ReportsStore from '../stores';

@withRouter
@observer
class NoChart extends Component {
  static propTypes = {
    type: PropTypes.string.isRequired,
  };

  handleClick = () => {
    const {
      type,
      history,
      location: { search },
    } = this.props;
    switch (type) {
      case 'env':
        history.push({
          pathname: '/devops/environment',
          search,
          state: { openCreate: true },
        });
        break;
      case 'app':
        history.push({
          pathname: '/devops/app-service',
          search,
          state: { openCreate: true },
        });
        break;
      default:
        break;
    }
  };

  render() {
    const { type } = this.props;
    const { getProRole } = ReportsStore;
    const noChart = {
      owner: {
        title: `report.no-${type}`,
        des: `report.no-${type}-des`,
      },
      member: {
        title: `report.member.no-${type}`,
        des: `report.member.no-${type}-des`,
      },
      '': {
        title: 'null',
        des: 'null',
      },
    };
    return (
      <div className="c7n-no-chart">
        <div className="c7n-no-chart-pic">
          <div />
        </div>
        <div className="c7n-no-chart-desc">
          <div className="c7n-no-chart-title"><FormattedMessage id={noChart[getProRole].title} /></div>
          <div className="c7n-no-chart-des"><FormattedMessage id={noChart[getProRole].des} /></div>
          {getProRole === 'owner' && (
            <Button
              type="primary"
              funcType="raised"
              onClick={this.handleClick}
            >
              <FormattedMessage id={`report.${type}.create`} />
            </Button>
          )}
        </div>
      </div>
    );
  }
}

export default NoChart;
