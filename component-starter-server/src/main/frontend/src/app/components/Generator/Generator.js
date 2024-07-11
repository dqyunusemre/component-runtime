/**
 *  Copyright (C) 2006-2024 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import React from 'react';
import { Outlet, Route, Routes } from 'react-router-dom';

import theme from './Generator.module.scss';

import ProjectMetadata from '../ProjectMetadata';
import Component from '../Component';
import ComponentAddForm from '../ComponentAddForm';

import Finish from '../Finish';
import SideMenu from '../SideMenu';
import DatastoreList from '../DatastoreList';
import DatasetList from '../DatasetList';

import { GENERATOR_ZIP_URL } from '../../constants';

export default function Generator() {
	return (
		<div className={theme.Generator}>
			<div className={theme.container}>
				<div className={theme.wizard}>
					<SideMenu />
				</div>
				<div className={theme.content}>
					<main>
						<Outlet />
					</main>
				</div>
			</div>
		</div>
	);
}
