import React from 'react';
import { Icon, Tooltip } from 'choerodon-ui';
import PropTypes from 'prop-types';
import './index.less';

const statusObj = {
  success: {
    icon: 'check_circle',
    text: '成功',
  },
  failed: {
    icon: 'cancel',
    text: '失败',
  },
  running: {
    icon: 'timelapse',
    text: '运行中',
  },
  canceled: {
    icon: 'cancle_b',
    text: '取消',
  },
  created: {
    icon: 'adjust',
    text: '未执行',
  },
  pending: {
    icon: 'timelapse',
    text: '准备中',
  },
  skipped: {
    icon: 'skipped_a',
    text: '已跳过',
  },
};

const statusDot = (props) => {
  const { size, status } = props;
  return (
    <Tooltip title={statusObj[status].text}>
      <Icon type={statusObj[status].icon} style={{ fontSize: `${size}px` }} />
    </Tooltip>
  );
};

statusDot.propTypes = {
  status: PropTypes.string.isRequired,
  size: PropTypes.number,
};
statusDot.defaultProps = {
  size: 12,
};


export default statusDot;
