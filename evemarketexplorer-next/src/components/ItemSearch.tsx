'use client';

import React, { useState, useEffect, useRef } from 'react';
import { Search, Loader2 } from 'lucide-react';
import { Input } from '@/components/ui/input';
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandItem,
  CommandList,
} from '@/components/ui/command';
import { Badge } from '@/components/ui/badge';
import { useDebounce } from 'use-debounce';
import { EveMarketApiService } from '@/services/api';
import type { ItemType } from '@/types/api';

interface ItemSearchProps {
  onItemSelect: (item: ItemType) => void;
  placeholder?: string;
  className?: string;
}

export function ItemSearch({ onItemSelect, placeholder = "Search for an item...", className }: ItemSearchProps) {
  const [inputValue, setInputValue] = useState('');
  const [items, setItems] = useState<ItemType[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedItem, setSelectedItem] = useState<ItemType | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const resultsRef = useRef<HTMLDivElement>(null);
  
  // Debounce the search query
  const [debouncedQuery] = useDebounce(inputValue, 300);

  // Search effect
  useEffect(() => {
    const searchItems = async () => {
      if (debouncedQuery.length < 2) {
        setItems([]);
        setIsOpen(false);
        return;
      }

      setLoading(true);
      try {
        const results = await EveMarketApiService.searchItems(debouncedQuery, 20);
        setItems(results);
        setIsOpen(results.length > 0);
      } catch (error) {
        console.error('Error searching items:', error);
        setItems([]);
        setIsOpen(false);
      } finally {
        setLoading(false);
      }
    };

    searchItems();
  }, [debouncedQuery]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        resultsRef.current &&
        !resultsRef.current.contains(event.target as Node) &&
        inputRef.current &&
        !inputRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (item: ItemType) => {
    setSelectedItem(item);
    setInputValue(item.name);
    setIsOpen(false);
    onItemSelect(item);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setInputValue(value);
    
    // Clear selection if user starts typing again
    if (selectedItem && value !== selectedItem.name) {
      setSelectedItem(null);
    }
    
    // Show dropdown when user starts typing
    if (value.length >= 2 && items.length > 0) {
      setIsOpen(true);
    }
  };

  const clearSelection = () => {
    setSelectedItem(null);
    setInputValue('');
    setItems([]);
    setIsOpen(false);
    inputRef.current?.focus();
  };

  return (
    <div className={`relative ${className}`}>
      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          ref={inputRef}
          value={inputValue}
          onChange={handleInputChange}
          placeholder={placeholder}
          className="pl-9 pr-4"
          onFocus={() => {
            if (inputValue.length >= 2 && items.length > 0) {
              setIsOpen(true);
            }
          }}
        />
        {loading && (
          <Loader2 className="absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-muted-foreground" />
        )}
      </div>

      {/* Search Results Dropdown */}
      {isOpen && (
        <div
          ref={resultsRef}
          className="absolute z-50 w-full mt-1 bg-popover border rounded-md shadow-md"
        >
          <Command shouldFilter={false}>
            <CommandList className="max-h-[300px] overflow-y-auto">
              {items.length === 0 ? (
                <CommandEmpty>
                  {debouncedQuery.length < 2 
                    ? "Type at least 2 characters to search..."
                    : "No items found."
                  }
                </CommandEmpty>
              ) : (
                <CommandGroup heading="Items">
                  {items.map((item) => (
                    <CommandItem
                      key={item.typeId}
                      value={item.name}
                      onSelect={() => handleSelect(item)}
                      className="flex flex-col items-start gap-1 p-3 cursor-pointer"
                    >
                      <div className="flex w-full items-center justify-between">
                        <span className="font-medium">{item.name}</span>
                        <Badge variant="secondary" className="text-xs">
                          {item.group.name}
                        </Badge>
                      </div>
                      {item.description && (
                        <p className="text-xs text-muted-foreground line-clamp-2">
                          {item.description}
                        </p>
                      )}
                    </CommandItem>
                  ))}
                </CommandGroup>
              )}
            </CommandList>
          </Command>
        </div>
      )}
      
      {/* Selected Item Badge */}
      {selectedItem && (
        <div className="mt-2 flex items-center gap-2">
          <Badge variant="outline" className="flex items-center gap-2">
            <span>Selected: {selectedItem.name}</span>
            <button
              onClick={clearSelection}
              className="ml-1 text-muted-foreground hover:text-foreground"
            >
              ×
            </button>
          </Badge>
        </div>
      )}
    </div>
  );
}

