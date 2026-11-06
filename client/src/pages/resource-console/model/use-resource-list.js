import { useEffect, useRef, useState } from 'react';
import { loadResourceList } from '../api/resource-api.js';

export function useResourceList(url, reloadKey) {
    const [filters, setFilters] = useState([]);
    const [sorts, setSorts] = useState([]);
    const [size, setSize] = useState(20);
    const [result, setResult] = useState(null);
    const [query, setQuery] = useState({ page: 1, size: 20, filters: [], sorts: [] });

    const draftRef = useRef({ size, filters, sorts });
    const reloadRef = useRef(reloadKey);

    useEffect(() => {
        draftRef.current = { size, filters, sorts };
    }, [size, filters, sorts]);

    useEffect(() => {
        const refreshed = reloadRef.current !== reloadKey;
        reloadRef.current = reloadKey;
        const currentQuery = refreshed ? { ...query, ...draftRef.current } : query;
        const controller = new AbortController();
        loadResourceList(url, currentQuery, controller.signal).then((response) => {
            if (!controller.signal.aborted) {
                setResult(response);
            }
        });
        return () => controller.abort();
    }, [url, query, reloadKey]);

    function load(page) {
        setQuery({ page, size, filters, sorts });
    }

    return { filters, setFilters, sorts, setSorts, size, setSize, result, load };
}
