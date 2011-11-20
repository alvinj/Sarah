-- takes an argument like 10 or 20 to decrease the volume

on run argv
  set decrease to item 1 of argv
  set current_volume to output volume of (get volume settings)

  if current_volume is greater than 0 then
    set current_volume to current_volume - decrease
  end if
  set volume output volume current_volume

  say "Decreased volume"
end run


