package com.spherelabs.error;

import lombok.Data;

public record APIError (

   String code,

   String message) {}