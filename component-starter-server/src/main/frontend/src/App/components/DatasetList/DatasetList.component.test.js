import React from 'react';
import { shallow } from 'enzyme';

import Component from './DatasetList.component';

describe('Component DatasetList', () => {
	it('should render', () => {
		const wrapper = shallow(
			<Component />
		);
		expect(wrapper.getElement()).toMatchSnapshot();
	});
});
