// Supplied (do not edit). First React components: props in, exact amounts out, conditional display.
import { render, screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import PaymentRow from '../../../src/components/PaymentRow.jsx';
import PaymentQueue from '../../../src/components/PaymentQueue.jsx';
import { sampleRequests } from './fixtures.js';

function renderRow(props) {
  return render(
    <table>
      <tbody>
        <PaymentRow {...props} />
      </tbody>
    </table>,
  );
}

export function componentSuite() {
  describe('PaymentRow renders one request from its props', () => {
    it('shows the supplier, invoice number, exact amount and status', () => {
      renderRow({ id: 7, supplierId: 'SUP-0042', invoiceNumber: 'INV 7781', amount: '1234.50', status: 'SUBMITTED' });
      const row = screen.getByRole('row');
      expect(row).toHaveTextContent('SUP-0042');
      expect(row).toHaveTextContent('INV 7781');
      expect(row).toHaveTextContent('1234.50');
      expect(row).toHaveTextContent('SUBMITTED');
    });

    it.each(['0.01', '0.10', '1000.00', '9999999.99', '20.05'])('shows the amount %s exactly as the API sent it', (amount) => {
      renderRow({ id: 1, supplierId: 'SUP-0001', invoiceNumber: 'INV-1', amount, status: 'APPROVED' });
      expect(screen.getByRole('row')).toHaveTextContent(amount);
    });

    it('re-renders when the props change', () => {
      const props = { id: 2, supplierId: 'SUP-0002', invoiceNumber: 'INV-2', amount: '5.00', status: 'SUBMITTED' };
      const { rerender } = renderRow(props);
      rerender(
        <table>
          <tbody>
            <PaymentRow {...props} status="REJECTED" amount="4.99" />
          </tbody>
        </table>,
      );
      expect(screen.getByRole('row')).toHaveTextContent('REJECTED');
      expect(screen.getByRole('row')).toHaveTextContent('4.99');
      expect(screen.getByRole('row')).not.toHaveTextContent('SUBMITTED');
    });
  });

  describe('PaymentQueue shows a table or an empty message', () => {
    it('shows "No payment requests" and no table when the list is empty', () => {
      render(<PaymentQueue requests={[]} />);
      expect(screen.getByText(/no payment requests/i)).toBeInTheDocument();
      expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('shows one row per request, in the given order, with exact amounts', () => {
      const requests = sampleRequests();
      render(<PaymentQueue requests={requests} />);
      const table = screen.getByRole('table');
      const body = within(table).getAllByRole('rowgroup').at(-1);
      const rows = within(body).getAllByRole('row');
      expect(rows).toHaveLength(requests.length);
      requests.forEach((request, index) => {
        expect(rows[index]).toHaveTextContent(request.invoiceNumber);
        expect(rows[index]).toHaveTextContent(request.amount);
        expect(rows[index]).toHaveTextContent(request.status);
      });
      expect(screen.queryByText(/no payment requests/i)).not.toBeInTheDocument();
    });

    it('has column headers so each value is labelled', () => {
      render(<PaymentQueue requests={sampleRequests().slice(0, 1)} />);
      const headers = screen.getAllByRole('columnheader').map((h) => h.textContent.toLowerCase());
      for (const name of ['supplier', 'invoice', 'amount', 'status']) {
        expect(headers.some((text) => text.includes(name)), `a column header containing "${name}"`).toBe(true);
      }
    });
  });
}
