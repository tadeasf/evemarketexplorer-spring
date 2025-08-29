'use client';

import React, { useState, useMemo } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ArrowUpDown, ArrowUp, ArrowDown, MapPin, Calendar, Package } from 'lucide-react';
import type { MarketOrder } from '@/types/api';

interface MarketOrderTableProps {
  orders: MarketOrder[];
  orderType: 'buy' | 'sell';
  className?: string;
}

type SortColumn = 'price' | 'volume' | 'location' | 'region' | 'issued';
type SortDirection = 'asc' | 'desc';

export function MarketOrderTable({ orders, orderType, className }: MarketOrderTableProps) {
  const [sortColumn, setSortColumn] = useState<SortColumn>('price');
  const [sortDirection, setSortDirection] = useState<SortDirection>(
    orderType === 'buy' ? 'desc' : 'asc'
  );

  const sortedOrders = useMemo(() => {
    return [...orders].sort((a, b) => {
      let aValue: any;
      let bValue: any;

      switch (sortColumn) {
        case 'price':
          aValue = a.price;
          bValue = b.price;
          break;
        case 'volume':
          aValue = a.volumeRemain;
          bValue = b.volumeRemain;
          break;
        case 'location':
          aValue = a.locationName;
          bValue = b.locationName;
          break;
        case 'region':
          aValue = a.region.name;
          bValue = b.region.name;
          break;
        case 'issued':
          aValue = new Date(a.issued);
          bValue = new Date(b.issued);
          break;
        default:
          return 0;
      }

      if (aValue < bValue) {
        return sortDirection === 'asc' ? -1 : 1;
      }
      if (aValue > bValue) {
        return sortDirection === 'asc' ? 1 : -1;
      }
      return 0;
    });
  }, [orders, sortColumn, sortDirection]);

  const handleSort = (column: SortColumn) => {
    if (column === sortColumn) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortColumn(column);
      // Default sort direction based on column
      setSortDirection(column === 'price' && orderType === 'buy' ? 'desc' : 'asc');
    }
  };

  const getSortIcon = (column: SortColumn) => {
    if (column !== sortColumn) {
      return <ArrowUpDown className="ml-2 h-4 w-4" />;
    }
    return sortDirection === 'asc' 
      ? <ArrowUp className="ml-2 h-4 w-4" />
      : <ArrowDown className="ml-2 h-4 w-4" />;
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'decimal',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(price);
  };

  const formatVolume = (volume: number) => {
    return new Intl.NumberFormat('en-US').format(volume);
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffInHours = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60));
    const diffInDays = Math.floor(diffInHours / 24);

    if (diffInDays === 0) {
      if (diffInHours === 0) {
        return 'Less than 1h ago';
      }
      return `${diffInHours}h ago`;
    } else if (diffInDays === 1) {
      return 'Yesterday';
    } else if (diffInDays < 7) {
      return `${diffInDays}d ago`;
    } else {
      return date.toLocaleDateString();
    }
  };


  if (orders.length === 0) {
    return (
      <div className="flex items-center justify-center p-8 text-muted-foreground">
        <div className="text-center">
          <Package className="mx-auto h-12 w-12 text-muted-foreground/50 mb-2" />
          <p>No {orderType} orders available</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`rounded-md border ${className}`}>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>
              <Button
                variant="ghost"
                size="sm"
                className="h-auto p-0 font-semibold"
                onClick={() => handleSort('price')}
              >
                Price (ISK)
                {getSortIcon('price')}
              </Button>
            </TableHead>
            <TableHead>
              <Button
                variant="ghost"
                size="sm"
                className="h-auto p-0 font-semibold"
                onClick={() => handleSort('volume')}
              >
                Volume
                {getSortIcon('volume')}
              </Button>
            </TableHead>
            <TableHead>
              <Button
                variant="ghost"
                size="sm"
                className="h-auto p-0 font-semibold"
                onClick={() => handleSort('location')}
              >
                Location
                {getSortIcon('location')}
              </Button>
            </TableHead>
            <TableHead>
              <Button
                variant="ghost"
                size="sm"
                className="h-auto p-0 font-semibold"
                onClick={() => handleSort('region')}
              >
                Region
                {getSortIcon('region')}
              </Button>
            </TableHead>
            <TableHead>
              <Button
                variant="ghost"
                size="sm"
                className="h-auto p-0 font-semibold"
                onClick={() => handleSort('issued')}
              >
                Issued
                {getSortIcon('issued')}
              </Button>
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {sortedOrders.slice(0, 50).map((order) => (
            <TableRow key={order.orderId}>
              <TableCell className="font-medium">
                <span className={orderType === 'buy' ? 'text-green-600' : 'text-red-600'}>
                  {formatPrice(order.price)}
                </span>
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-1">
                  <Package className="h-3 w-3 text-muted-foreground" />
                  {formatVolume(order.volumeRemain)}
                </div>
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-1">
                  <MapPin className="h-3 w-3 text-muted-foreground" />
                  <span className="text-sm text-muted-foreground">
                    {order.locationName}
                  </span>
                </div>
              </TableCell>
              <TableCell>
                <Badge variant="outline" className="text-xs">
                  {order.region.name}
                </Badge>
              </TableCell>
              <TableCell>
                <div className="flex items-center gap-1">
                  <Calendar className="h-3 w-3 text-muted-foreground" />
                  <span className="text-sm text-muted-foreground">
                    {formatDate(order.issued)}
                  </span>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      
      {orders.length > 50 && (
        <div className="border-t px-4 py-3 text-center text-sm text-muted-foreground">
          Showing top 50 of {formatVolume(orders.length)} {orderType} orders
        </div>
      )}
    </div>
  );
}