import {screen, waitFor, within} from "@testing-library/vue";
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ref} from 'vue';

import {useApplicationStore} from '@/composables/useApplicationStore';
import Application from '@/services/application';
import Instance from '@/services/instance';
import {render} from '@/test-utils';
import Wallboard from '@/views/wallboard/index.vue';

vi.mock('@/composables/useApplicationStore', () => ({
    useApplicationStore: vi.fn(),
}));

describe('Wallboard', () => {
    let applicationsInitialized;
    let applications;
    let error;

    beforeEach(async () => {
        applicationsInitialized = ref(false);
        applications = ref([]);
        error = ref(null);

        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        useApplicationStore.mockReturnValue({
            applicationsInitialized,
            applications,
            error,
            applicationStore: {
                findApplicationByInstanceId: (id: string) => ({})
            }
        });

        render(Wallboard);
    });

    it('shows message when applications list is loaded', async () => {
        screen.findByText('applications.loading_applications');
    });

    it('shows application', async () => {
        applicationsInitialized.value = true;
        applications.value = [
            new Application({
                name: 'Test Application',
                statusTimestamp: Date.now(),
                instances: [new Instance({id: '4711', registration: {name: 'Test Application'}})],
            }),
        ];
        await waitFor(() => {
            expect(screen.getByText('Test Application')).toBeVisible();
        });
    });

    it('shows error, when connection is lost', async () => {
        applicationsInitialized.value = true;
        applications.value = [];
        error.value = new Error('Connection lost');
        await waitFor(() => {
            expect(screen.getByText('Server connection failed.')).toBeVisible();
        });
    });

    it('does not show duplicated entries in status filter', async () => {
        applications.value = [
            new Application({
                name: 'Test Application',
                statusTimestamp: Date.now(),
                instances: [new Instance({id: '4711'})],
                status: 'UP'
            }),
            new Application({
                name: 'Test Application 2',
                statusTimestamp: Date.now(),
                instances: [new Instance({id: '4712'})],
                status: 'UP'
            }),
            new Application({
                name: 'Test Application Down',
                statusTimestamp: Date.now(),
                instances: [new Instance({id: '4713'})],
                status: 'DOWN'
            }),
        ];

        await waitFor(() => {
            let dropdown = screen.queryByLabelText('status-filter');
            expect(within(dropdown).getAllByRole('option')).toHaveLength(3);
            expect(within(dropdown).getByRole('option', {name: 'all'})).toBeVisible();
            expect(within(dropdown).getByRole('option', {name: 'up'})).toBeVisible();// expecting only one!
            expect(within(dropdown).getByRole('option', {name: 'down'})).toBeVisible();
        });
    });
});
