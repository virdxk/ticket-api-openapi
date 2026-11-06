import { request } from '../../../shared/api/index.js';

export const API_BASE = import.meta.env.VITE_API_BASE ?? '';

export const resourceApi = {
    get: (url) => request('GET', url),
    remove: (url) => request('DELETE', url),
    update: (url, body) => request('PATCH', url, body, 'application/merge-patch+json'),
    create: (url, body) => request('POST', url, body),
    execute: (action, values) => request(action.method, action.path(values.map((value) => encodeURIComponent(value.trim())))),
};

export function loadResourceList(url, query, signal) {
    const params = new URLSearchParams({ page: query.page, size: query.size });
    query.sorts.forEach((sort) => params.append('sort', `${sort.field},${sort.dir}`));
    query.filters.forEach((filter) => params.append('filter', `${filter.field}${filter.op}${filter.value}`));
    return request('GET', `${url}?${params}`, undefined, 'application/json', signal);
}
