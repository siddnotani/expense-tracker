export function formatMinor(minor: number, currency: string): string {
  const integerMinor = Math.trunc(minor);
  const negative = integerMinor < 0;
  const absoluteMinor = Math.abs(integerMinor);
  const whole = Math.trunc(absoluteMinor / 100);
  const cents = String(absoluteMinor % 100).padStart(2, '0');
  const formatter = new Intl.NumberFormat(undefined, {
    style: 'currency',
    currency: currency || 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  const parts = formatter.formatToParts(negative ? -0 : 0);
  const groupedWhole = new Intl.NumberFormat(undefined).format(whole);
  return parts
    .map((part) => {
      if (part.type === 'integer') {
        return groupedWhole;
      }
      if (part.type === 'fraction') {
        return cents;
      }
      return part.value;
    })
    .join('');
}

export function parseMajorToMinor(input: string): number {
  const value = input.trim();
  if (!/^[+-]?\d+(?:[.,]\d{0,2})?$/.test(value)) {
    throw new Error('Enter an amount with no more than two decimal places.');
  }
  const sign = value.startsWith('-') ? -1 : 1;
  const unsigned = value.replace(/^[+-]/, '');
  const [whole, fraction = ''] = unsigned.split(/[.,]/);
  const minor = BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0') || '0');
  const result = Number(minor * BigInt(sign));
  if (!Number.isSafeInteger(result)) {
    throw new Error('Enter a smaller amount.');
  }
  return result;
}

export function minorToMajorInput(minor: number): string {
  const integerMinor = Math.trunc(minor);
  const negative = integerMinor < 0;
  const absoluteMinor = Math.abs(integerMinor);
  const whole = Math.trunc(absoluteMinor / 100);
  const cents = String(absoluteMinor % 100).padStart(2, '0');
  return `${negative ? '-' : ''}${whole}.${cents}`;
}

export function signedMinor(direction: string, amountMinor: number): number {
  return direction === 'INCOME' ? amountMinor : -amountMinor;
}
